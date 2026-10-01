#!/usr/bin/env python3
"""比对代码侧 OpenAPI spec 与 Apifox 已导入接口，报告会漂移的字段。

只读、纯离线：输入两份 JSON，不联网、不写 Apifox。存在的理由是——Apifox 的「智能合并」导入
会保留接口侧已有的中文名与说明，所以代码改 @Operation 文本永远不会流进去（这条实测记在
docs/modules/admin-service.md）。没有这支脚本，那种滞后只能靠人记得。

用法（endpoint list 不含 description，所以逐条 get 后首尾相连即可）：
    ids=$(apifox endpoint list --project 8871910 --page-size 500 \
            | python3 -c "import json,sys;print(' '.join(str(r['id']) for r in json.load(sys.stdin)['data']))")
    for id in $ids; do apifox endpoint get "$id" --project 8871910; done > endpoints.json
    ./gradlew :admin-service:test --tests OpenApiExportTest -PintegrationTests
    python3 gradle/apifox/doc-drift-check.py \\
        --spec admin-service/build/api-docs/openapi.json \\
        --apifox endpoints.json

退出码：0 = 无漂移；1 = 有文本漂移 / 示例重复或与 spec 不一致 / --fail-on-missing 时接口集合不一致；2 = 输入不可用。
"""

import argparse
import json
import sys

# 文本比对只看这两项（spec 的 summary 已归一化成 name）；参数与 schema 随导入覆盖，不在范围。
# 响应示例另走 check_examples：它不会被 merge 导入更新，只会逐次追加，必须单独检。
TEXT_FIELDS = ("name", "description")


def load_json(path, label):
    """读一份 JSON，也接受「多份 pretty-printed JSON 首尾相连」——那正是逐条 apifox endpoint get 的输出形状。"""
    try:
        with open(path, encoding="utf-8") as handle:
            text = handle.read()
    except OSError as error:
        print(f"[错误] 读不了{label}：{error}", file=sys.stderr)
        raise SystemExit(2)

    decoder = json.JSONDecoder()
    documents = []
    index = 0
    while index < len(text):
        while index < len(text) and text[index].isspace():
            index += 1
        if index >= len(text):
            break
        try:
            value, index = decoder.raw_decode(text, index)
        except json.JSONDecodeError as error:
            print(f"[错误] {label} 在偏移 {index} 处不是合法 JSON：{error.msg}", file=sys.stderr)
            raise SystemExit(2)
        documents.append(value)

    if len(documents) == 1:
        return documents[0]
    merged = []
    for document in documents:
        merged.append(document.get("data", document) if isinstance(document, dict) else document)
    return merged


def spec_operations(spec):
    """(METHOD path) -> {name, description}，取 springdoc 的 summary/description。"""
    operations = {}
    for path, item in (spec.get("paths") or {}).items():
        for method, operation in item.items():
            if method.upper() not in {"GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"}:
                continue
            operations[f"{method.upper()} {path}"] = {
                "name": (operation.get("summary") or "").strip(),
                "description": (operation.get("description") or "").strip(),
                "examples": {
                    code: content.get("application/json", {}).get("example")
                    for code, resp in (operation.get("responses") or {}).items()
                    for content in [resp.get("content") or {}]
                },
            }
    return operations


def apifox_endpoints(payload):
    """容忍三种输入：CLI 信封 {data:[...]}、{data:{...}}、裸列表。"""
    rows = payload.get("data", payload) if isinstance(payload, dict) else payload
    if isinstance(rows, dict):
        rows = [rows]
    # 逐条解掉 CLI 信封：{success, data:{...}} -> {...}。顶层是数组时上面那行取不到 data，必须在这里补，
    # 否则每一条都被下面的 path/method 过滤掉，表现是「接口数为 0」这种看起来像没漂移的假绿。
    rows = [row.get("data", row) if isinstance(row, dict) else row for row in rows or []]
    endpoints = {}
    for row in rows or []:
        if not isinstance(row, dict) or "path" not in row or "method" not in row:
            continue
        key = f"{row['method'].upper()} {row['path']}"
        endpoints[key] = {
            "id": row.get("id"),
            "name": (row.get("name") or "").strip(),
            "description": (row.get("description") or "").strip(),
            "responses": row.get("responses") or [],
            "responseExamples": row.get("responseExamples") or [],
        }
    return endpoints



def check_examples(key, actual, expected):
    """检查响应示例：同一个响应码上挂了几份、内容是否与 spec 一致。

    <p>为什么必须查这个：`--overwrite-mode merge` 每次导入都会**追加**一份示例而不是更新已有的，
    实测 23 个接口攒到 104 份示例（清理后应为 54 份，正好等于带 example 的响应数）。
    重复示例不只是难看——Mock 的优先级里「响应示例」高于智能 Mock，留着一份内容过期的示例
    等于让 Mock 可能返回一个代码里已经不存在的文案。
    """
    problems = []
    by_response_id = {str(r.get("id")): str(r.get("code")) for r in actual["responses"]}
    per_code = {}
    for item in actual["responseExamples"]:
        code = by_response_id.get(str(item.get("responseId")))
        if code is None:
            problems.append(f"{key}: 示例「{item.get('name')}」没有绑定到任何响应（responseId 对不上）")
            continue
        per_code.setdefault(code, []).append(item)
    for code, items in sorted(per_code.items()):
        if len(items) > 1:
            problems.append(f"{key}: {code} 挂了 {len(items)} 份示例（重复导入攒出来的）")
        want = (expected.get("examples") or {}).get(code)
        if want is None:
            continue
        for item in items:
            try:
                got = json.loads(item.get("data") or "{}")
            except json.JSONDecodeError:
                problems.append(f"{key}: {code} 的示例「{item.get('name')}」不是合法 JSON")
                continue
            if got != want:
                problems.append(f"{key}: {code} 的示例内容与 spec 不一致 —— 存的是 {json.dumps(got, ensure_ascii=False)[:60]}"
                                f"，代码是 {json.dumps(want, ensure_ascii=False)[:60]}")
    return problems


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--spec", required=True, help="代码侧 openapi.json")
    parser.add_argument("--apifox", required=True, help="apifox endpoint list 的 JSON 输出")
    parser.add_argument("--project", type=int, help="给出后为每条漂移打印可直接执行的修复命令")
    parser.add_argument("--fail-on-missing", action="store_true", help="接口集合不一致也算失败")
    parser.add_argument("--warn-only", action="store_true", help="有漂移也只报告，退出码恒为 0")
    args = parser.parse_args()

    expected = spec_operations(load_json(args.spec, "spec"))
    actual = apifox_endpoints(load_json(args.apifox, "Apifox 导出"))

    only_in_apifox = sorted(set(actual) - set(expected))
    only_in_spec = sorted(set(expected) - set(actual))
    drifts = []
    for key in sorted(set(expected) & set(actual)):
        for field in TEXT_FIELDS:
            want, got = expected[key][field], actual[key][field]
            if want != got:
                kind = "代码领先（Apifox 是其前缀）" if got and want.startswith(got) else "两边不同文"
                drifts.append((key, field, kind, want, got))

    print(f"spec 操作数 {len(expected)} ｜ Apifox 接口数 {len(actual)}")
    for key, field, kind, want, got in drifts:
        print(f"\n[漂移] {key} 的 {field}：{kind}")
        print(f"  代码 ：{want[:160]}")
        print(f"  Apifox：{got[:160]}")
        if args.project:
            print(f"  修复 ：apifox endpoint update {actual[key]['id']} --project {args.project} "
                  f"--branch main --{'name' if field == 'name' else 'description'} \"<代码文本>\"")
    for key in only_in_apifox:
        print(f"[仅 Apifox] {key}（代码已删除该接口，或它不是导入产生的）")
    for key in only_in_spec:
        print(f"[仅代码]   {key}（尚未导入 Apifox）")

    example_problems = []
    for key in sorted(set(expected) & set(actual)):
        example_problems.extend(check_examples(key, actual[key], expected[key]))
    for line in example_problems:
        print(f"[示例] {line}")

    set_mismatch = bool(only_in_apifox or only_in_spec)
    if not drifts and not example_problems and not (args.fail_on_missing and set_mismatch):
        print("无文本漂移，示例也无重复且与 spec 一致。")
        return 0
    return 0 if args.warn_only else 1


if __name__ == "__main__":
    sys.exit(main())
