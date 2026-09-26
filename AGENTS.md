# Coder Development Guidelines

Make the smallest correct change, follow existing patterns, and verify the result. Ask only when the request is unclear, a meaningful design choice remains, or the action is destructive. If you want an exception to any rule in these documents, stop and get explicit permission first.

Prioritize correctness over agreement. State uncertainty instead of guessing, and push back on technically unsound requests with evidence.

## Prerequisites

- **Java**: JDK 25 via `JAVA_HOME`;
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
  touch persistence entities, `@Transactional` only appears in `service`, each `*Controller`/`*Service`/`*Repository`/
  `*Entity` resides in its own package, package slices stay acyclic, `common-core` never depends on a business
  service or on the gateway, and `common-security` stays free of any transport or persistence stack — no Servlet,
  no Spring Web, no JPA, no Spring Data, no `@Transactional` — and never points back at `common-core`. That is what
  lets the reactive gateway reuse the same signing code without a list of `exclude` rules; what the kernel contains
  is inventoried in `common-security/README.md`, not here.
- **Coding hygiene** (ArchUnit, asserted per module): constructor injection only, so `@Autowired`/`@Resource`/`@Inject`
  never sit on a field; no `System.out`/`System.err`/`printStackTrace`, because only the logging framework carries the
  traceId from P3-1; no `new Date()`, while `java.util.Date` itself stays legal for jjwt's `issuedAt`/`expiration`.
- **SpotBugs**: `Effort.DEFAULT` + `Confidence.MEDIUM`, failures block. Reports land in
  `<module>/build/reports/spotbugs/main.{html,xml}`. Prefer fixing code; a new exemption belongs in
  `gradle/spotbugs/exclude.xml` with a comment justifying it, and must stay narrow (rule + package, never a blanket
  category).
- **Tests**: anything needing local MySQL/Nacos/Redis carries `@Tag("integration")` so `test`/`check` stay runnable on a
  clean machine. New tests should avoid containers by default.
- **Formatting**: Spotless covers `*.java` (palantir), `*.md` (Prettier + markdownlint), `*.gradle.kts` (whitespace and
  final newline only, existing tab indentation is kept) and `*.yml` (Prettier). Flyway `db/migration/*.sql` is
  deliberately left unformatted so migration diffs stay reviewable. `.editorconfig` matches what those formatters
  actually emit: Java 4 spaces / 120 columns, YAML 2 spaces, kts keeps tabs, LF everywhere.

## Documentation map

This file is the entry point: the command table and the enforced conventions above exist nowhere else. Every other
document has one job, and a fact written down in two places is a fact that will drift.

| Document                   | Answers                                                     | Never contains                        |
| -------------------------- | ----------------------------------------------------------- | ------------------------------------- |
| `README.md` (root)         | What the platform is, the module panorama, how to run it    | Progress, plans, dates, gate rules    |
| `AGENTS.md` (this file)    | Commands, gates, hard conventions, this routing table       | Module internals, design history      |
| `docs/architecture.md`     | Cross-module contracts that fail silently if one side moves | Single-module implementation details  |
| `<module>/README.md`       | Facts and conclusions of one module                         | Rationale, rejected options, pitfalls |
| `docs/modules/<module>.md` | Why it was chosen, what was rejected, what bit us           | Anything already stated elsewhere     |
| `docs/quality-gates.md`    | What a gate technology is and how it is wired in            | The rule inventory                    |
| `SECURITY.md`              | Vulnerability reporting and secret handling                 | Architecture detail                   |

- **Module README skeleton** — a lead paragraph stating the module's job, then `能力与接口口径` / `关键实现` /
  `架构约束` / `主要依赖` / `命令`. Facts and conclusions only, every entry at most two lines; anything that needs a
  "because" goes to `docs/modules/<module>.md`.
- **No second inventory**: never hand-copy a list the code already publishes (routes and schemas come from
  `/v3/api-docs`, the rule list from the module's `ArchitectureTest`). Link to the source of truth instead.
- **A pitfall entry carries its own deletion condition**: once a gate or a test makes the mistake impossible, delete
  the entry rather than let it age.
- **No line-number references**: cite `path + class#method` or a configuration key. Line numbers rot on the next edit.
- **Naming and layout**: prose in Chinese, file names in English kebab-case, one document per topic — no `v2` copies of
  a directory, no index files over a closed document set (the table above is the whole inventory).
- **Private workspaces** (`.ai/`, `.trae/`) are not tracked and are never referenced by committed documentation.

## Workflow

- Inspect the working tree before editing. For an existing PR, check out its branch first.
- Discuss architectural decisions such as framework changes, major refactoring, and system design before implementing them. Routine fixes and clear implementations do not need discussion.
- When asked a question, answer the question instead of jumping to implementation.
- Install the repository Git hooks once per clone: any Gradle invocation (e.g. `./gradlew help`) generates `.git/hooks/pre-commit` (`spotlessCheck`, ~2s on a warm daemon), `commit-msg` (conventional commits), and `pre-push` (`prePushCheck`) from the `gitHooks` block in `settings.gradle.kts`.
- Never edit the generated hooks by hand, and never bypass them with `--no-verify`. Every Gradle invocation regenerates them from the script declaration, so local edits are silently lost; only these three are generated, leaving a third-party `post-commit`/`post-checkout` alone. Where `.git` is not writable (agent sandboxes, source archives) pass `-PskipGitHooks`. Wait for slow first runs while caches warm.
- Prefer targeted tests and checks while iterating. Run the broader checks required by the affected area before handoff.
- Do not force-push unless explicitly requested.
- Commit and PR titles use `type(scope): message`. A scope must be a real path containing every changed file. Use a broader scope or no scope for cross-cutting changes.
