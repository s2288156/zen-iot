/**
 * 把可选文本框的值归一成后端要的 `string | null`。
 *
 * 后端对 `nickname` / `email` / `phone` / `description` 这类字段只约束长度与格式、不约束非空，
 * 传 `''` 会**原样存成空串**而不是 NULL（`UserUpdateRequest`、`RoleUpdateRequest` 皆如此），
 * 于是「清空」和「没填」在库里变成两种状态，查询与展示都要多判一次。统一在提交前归一成 `null`。
 *
 * 顺带 `trim`：后端不做首尾空白处理，`" admin "` 会连着空格一起入库并参与唯一性判定。
 */
export function textOrNull(value: string | undefined): string | null {
  const trimmed = value?.trim()
  return trimmed === undefined || trimmed === '' ? null : trimmed
}
