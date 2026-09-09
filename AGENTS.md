# Coder Development Guidelines

Make the smallest correct change, follow existing patterns, and verify the result. Ask only when the request is unclear, a meaningful design choice remains, or the action is destructive. If you want an exception to any rule in these documents, stop and get explicit permission first.

Prioritize correctness over agreement. State uncertainty instead of guessing, and push back on technically unsound requests with evidence.

## Prerequisites

- **Java**: JDK 21 via `JAVA_HOME`;
- **Build tooling**: Gradle 9.7.1
- **Node.js**: 20 or newer (Spotless Prettier and markdownlint-cli run through `npx`; the first run needs network)
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
| Push gate     | `./gradlew prePushCheck`      | Format + Markdown lint + compile every module, without running tests      |

## Workflow

- Inspect the working tree before editing. For an existing PR, check out its branch first.
- Discuss architectural decisions such as framework changes, major refactoring, and system design before implementing them. Routine fixes and clear implementations do not need discussion.
- When asked a question, answer the question instead of jumping to implementation.
- Install the repository Git hooks once per clone: any Gradle invocation (e.g. `./gradlew help`) generates `.git/hooks/pre-commit` (`spotlessCheck`), `commit-msg` (conventional commits), and `pre-push` (`prePushCheck`) from the `gitHooks` block in `settings.gradle.kts`.
- Never edit the generated hooks by hand, and never bypass them with `--no-verify`. Where `.git` is not writable (agent sandboxes, source archives) pass `-PskipGitHooks`. Wait for slow first runs while caches warm.
- Prefer targeted tests and checks while iterating. Run the broader checks required by the affected area before handoff.
- Do not force-push unless explicitly requested.
- Commit and PR titles use `type(scope): message`. A scope must be a real path containing every changed file. Use a broader scope or no scope for cross-cutting changes.
