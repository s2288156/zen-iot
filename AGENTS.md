# Coder Development Guidelines

Make the smallest correct change, follow existing patterns, and verify the result. Ask only when the request is unclear, a meaningful design choice remains, or the action is destructive. If you want an exception to any rule in these documents, stop and get explicit permission first.

Prioritize correctness over agreement. State uncertainty instead of guessing, and push back on technically unsound requests with evidence.

## Prerequisites

- **Java**: JDK 21 via `JAVA_HOME`;
- **Build tooling**: Gradle 9.7.1
- **Node.js**: 20 or newer (Spotless Prettier and markdownlint-cli run through `npx`; the first run needs network)
- Git

## Essential commands

| Task              | Command                             | Description                                                                          |
| ----------------- | ----------------------------------- | ------------------------------------------------------------------------------------ |
| Format all        | `./gradlew spotlessApply`           | Format Java + Markdown + kts + YAML in-place, then auto-fix Markdown lint issues     |
| Check format      | `./gradlew spotlessCheck`           | Verify formatting without modifying files                                            |
| Format Java       | `./gradlew spotlessJavaApply`       | Format Java files only                                                               |
| Lint Markdown     | `./gradlew lintMarkdown`            | Check Markdown conventions via markdownlint                                          |
| Fix Markdown      | `./gradlew lintMarkdownFix`         | Auto-fix Markdown conventions via markdownlint --fix                                 |
| Unit tests        | `./gradlew test`                    | Run tests that need no local middleware (integration tests are tagged out)           |
| Integration tests | `./gradlew test -PintegrationTests` | Include the `@Tag("integration")` tests; requires local MySQL/Nacos/Redis containers |
| Architecture      | `./gradlew architectureTest`        | ArchUnit layering and dependency-direction rules, no Spring context                  |
| Defect scan       | `./gradlew spotbugsMain`            | SpotBugs over main sources; `spotbugsTest` also runs in `check`                      |
| Full check        | `./gradlew check`                   | Format + Markdown lint + compile (-Werror) + SpotBugs + architecture + unit tests    |
| Push gate         | `./gradlew prePushCheck`            | Format + Markdown lint + compile (-Werror) + SpotBugs(main) + architecture rules     |

## Enforced conventions

Every rule below has a gate behind it, so a workaround shows up as a failed push rather than a review comment.

- **Zero compiler warnings**: `-Xlint:deprecation,unchecked -Werror` on every module. Do not switch it to
  `-Xlint:all` — Lombok then emits `No processor claimed any of these annotations`, which `-Werror` turns into a
  build failure. Suppress only with a reason.
- **Nullability**: `org.springframework.lang.Nullable`/`NonNull` are deprecated in Spring Framework 7. Use
  `org.jspecify.annotations.*` when an annotation is needed; both `-Werror` and an ArchUnit rule reject the Spring ones.
- **Layering** (ArchUnit, `architectureTest`): `controller → service → repository → entity` one-way, controllers never
  touch persistence entities, `@Transactional` only appears in `service`, `common-core` never depends on a business
  service, and `com.zen.common.core.jwt` stays free of the Servlet API so the Phase 2 gateway can reuse it.
- **SpotBugs**: `Effort.DEFAULT` + `Confidence.MEDIUM`, failures block. Prefer fixing code; a new exemption belongs in
  `gradle/spotbugs/exclude.xml` with a comment justifying it, and must stay narrow (rule + package, never a blanket
  category). `VerifiedToken` shed 4 findings by adding a `List.copyOf` compact constructor instead of an exemption.
- **Tests**: anything needing local MySQL/Nacos/Redis carries `@Tag("integration")` so `test`/`check` stay runnable on a
  clean machine. New tests should avoid containers by default.
- **Formatting**: Spotless covers `*.java` (palantir), `*.md` (Prettier + markdownlint), `*.gradle.kts` (whitespace and
  final newline only, existing tab indentation is kept) and `*.yml` (Prettier). Flyway `db/migration/*.sql` is
  deliberately left unformatted so migration diffs stay reviewable.

## Workflow

- Inspect the working tree before editing. For an existing PR, check out its branch first.
- Discuss architectural decisions such as framework changes, major refactoring, and system design before implementing them. Routine fixes and clear implementations do not need discussion.
- When asked a question, answer the question instead of jumping to implementation.
- Install the repository Git hooks once per clone: any Gradle invocation (e.g. `./gradlew help`) generates `.git/hooks/pre-commit` (`spotlessCheck`), `commit-msg` (conventional commits), and `pre-push` (`prePushCheck`) from the `gitHooks` block in `settings.gradle.kts`.
- Never edit the generated hooks by hand, and never bypass them with `--no-verify`. Where `.git` is not writable (agent sandboxes, source archives) pass `-PskipGitHooks`. Wait for slow first runs while caches warm.
- Prefer targeted tests and checks while iterating. Run the broader checks required by the affected area before handoff.
- Do not force-push unless explicitly requested.
- Commit and PR titles use `type(scope): message`. A scope must be a real path containing every changed file. Use a broader scope or no scope for cross-cutting changes.
