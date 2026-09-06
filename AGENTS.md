# Coder Development Guidelines

Make the smallest correct change, follow existing patterns, and verify the result. Ask only when the request is unclear, a meaningful design choice remains, or the action is destructive. If you want an exception to any rule in these documents, stop and get explicit permission first.

Prioritize correctness over agreement. State uncertainty instead of guessing, and push back on technically unsound requests with evidence.

## Prerequisites

- **Java**: JDK 21 via `JAVA_HOME`;
- **Build tooling**: Gradle 9.7.1
- Git

## Essential commands

| Task          | Command                       | Description                                                               |
| ------------- | ----------------------------- | ------------------------------------------------------------------------- |
| Format all    | `./gradlew spotlessApply`     | Format Java + Markdown files in-place, then auto-fix Markdown lint issues |
| Check format  | `./gradlew spotlessCheck`     | Verify formatting without modifying files                                 |
| Format Java   | `./gradlew spotlessJavaApply` | Format Java files only                                                    |
| Lint Markdown | `./gradlew lintMarkdown`      | Check Markdown conventions via markdownlint                               |
| Fix Markdown  | `./gradlew lintMarkdownFix`   | Auto-fix Markdown conventions via markdownlint --fix                      |
| Full check    | `./gradlew check`             | Run spotlessCheck + lintMarkdown + test                                   |
