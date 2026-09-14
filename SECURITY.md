# 安全政策

## 漏洞反馈

如发现本项目存在安全漏洞，请通过 GitHub 的 **Security Advisories** 功能提交私有报告（Settings → Security → Private vulnerability reporting）。不要在公开 Issue 或 PR 中披露细节。

反馈请包含：

- 漏洞类型（如 SQL 注入、权限绕过、敏感信息泄露）
- 受影响组件与版本范围
- 最小复现步骤或概念验证（PoC），**不要包含真实生产数据**
- 风险评估（CVSS 评分或自评级）

## 密钥泄露处置

本仓库通过 `.github/workflows/ci.yml` 中的 **gitleaks** job 在每次 push 时扫描，防止硬编码密钥入库。

若发生密钥泄露：

1. 立即在密钥所在平台（阿里云、GitHub、数据库等）**撤销并重新生成**
2. 用 `git filter-repo` 或 BFG Repo-Cleaner 从 Git 历史中移除；**不要只靠 force-push**
3. 在本文件中追加事件记录（泄露日期、密钥类型、处置措施）

## 依赖安全

- GitHub Actions 上的 OSV scanner（`osv-scanner`）定期扫描依赖漏洞
- 修复以 **CVSS ≥ 7.0**（高危 / 严重）为优先级；Dependabot 会自动生成修复 PR

## 责任范围

本项目为个人实验仓库，不提供 SLA；安全响应时间通常在 48 小时内。
