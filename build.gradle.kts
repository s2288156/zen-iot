import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.SpotBugsExtension
import com.github.spotbugs.snom.SpotBugsTask
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension

plugins {
	java
	id("io.spring.dependency-management") version "1.1.7" apply false
	id("org.springframework.boot") version "4.1.1" apply false
	id("com.diffplug.spotless") version "8.10.3"
	id("com.github.spotbugs") version "6.5.12" apply false
}

group = "com.zen"
version = "0.0.1-SNAPSHOT"

// 依赖本机中间件（MySQL/Nacos/Redis/RabbitMQ）的测试一律打 @Tag("integration")，默认从 test/check 排除，
// 这样 check 在干净环境下也是可执行的真门禁；容器就绪时用 ./gradlew test -PintegrationTests 纳入
val integrationTag = "integration"
val runIntegrationTests = project.hasProperty("integrationTests")

// 版本一致性门禁的两个输入：
// - sameVersionGroups：同一 group 内的构件必须同版本。触发源是"单条钉住组内一个构件"的约束——
//   Dependabot 每周一抬它却不抬 BOM 管的兄弟构件，#15 与 #20 两次把 spotbugs 配置拆成 core 2.26.1 + api 2.25.5。
//   根脚本现在不留这种约束，门禁守的是将来再有人加。
// - coherenceConfigurations：跨模块比较要看的配置。testRuntimeClasspath 是这里最有意义的一条，
//   库模块的自测运行时一旦和应用的不是同一套，CI 绿着也能藏住序列化行为差异。
val sameVersionGroups = listOf("org.apache.logging.log4j")
val coherenceConfigurations =
	listOf("annotationProcessor", "compileClasspath", "runtimeClasspath", "testRuntimeClasspath", "spotbugs")
val versionReportPath = "reports/dependency-versions.txt"

subprojects {
	apply(plugin = "java")
	apply(plugin = "jacoco")
	apply(plugin = "io.spring.dependency-management")
	apply(plugin = "com.github.spotbugs")

	// 版本只在根声明一次，子模块依赖一律不写版本号；接入新的第三方栈时在此追加 BOM
	configure<DependencyManagementExtension> {
		imports {
			mavenBom("org.springframework.boot:spring-boot-dependencies:4.1.1")
			mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.1.3")
			mavenBom("com.alibaba.cloud:spring-cloud-alibaba-dependencies:2025.1.0.0")
			// common-security 以 api 暴露 jjwt 类型，版本必须在根统一管；模块局部 BOM 不会传递给消费方
			mavenBom("io.jsonwebtoken:jjwt-bom:0.13.0")
		}
		// ArchUnit 没有 BOM，用单条约束把版本留在根，模块仍不写版本号
		dependencies {
			dependency("com.tngtech.archunit:archunit-junit5:1.5.1")
		}
	}

	group = "com.zen"
	version = "0.0.1-SNAPSHOT"

	configure<JavaPluginExtension> {
		toolchain {
			languageVersion.set(JavaLanguageVersion.of(25))
		}
	}

	dependencies {
		testImplementation("com.tngtech.archunit:archunit-junit5")
	}

	tasks.withType<JavaCompile>().configureEach {
		// 不取 -Xlint:all：Lombok 的「No processor claimed any of these annotations」会稳定告警，
		// 叠加 -Werror 必然失败；deprecation + unchecked 已能拦住 Spring 7 废弃 API（如 org.springframework.lang.Nullable）
		options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked", "-Werror"))
	}

	tasks.withType<Test>().configureEach {
		useJUnitPlatform {
			if (!runIntegrationTests) {
				excludeTags(integrationTag)
			}
		}
	}

	// 架构门禁单独成任务：只跑 @Tag("architecture")，秒级且不碰中间件，可安全进 pre-push
	val testSourceSet = sourceSets.getByName("test")
	tasks.register<Test>("architectureTest") {
		group = "verification"
		description = "Architecture constraint tests (ArchUnit, no local middleware needed)"
		testClassesDirs = testSourceSet.output.classesDirs
		classpath = testSourceSet.runtimeClasspath
		dependsOn(tasks.named("testClasses"))
		useJUnitPlatform {
			includeTags("architecture")
		}
	}

	// 6.5.x 默认不注册任何报告，失败时只会看到 exit code 1；这里显式建 HTML + XML 两类报告
	tasks.withType<SpotBugsTask>().configureEach {
		reports.create("html") { required.set(true) }
		reports.create("xml") { required.set(true) }
	}

	// 缺陷扫描：6.5.x 的 Threshold 已改名 Confidence
	configure<SpotBugsExtension> {
		toolVersion.set("4.10.4")
		effort.set(Effort.DEFAULT)
		reportLevel.set(Confidence.MEDIUM)
		ignoreFailures.set(false)
		excludeFilter.set(rootProject.layout.projectDirectory.file("gradle/spotbugs/exclude.xml"))
	}

	// 只解析本模块的配置：并行执行下跨项目解析会被 Gradle 拒（attempted without an exclusive lock）。
	// 所以比对分两步——本任务断言"同组同版本"并写出已解析版本报告，根任务 crossModuleVersionCheck 读报告做跨模块比较。
	// 故意不声明 outputs：依赖版本变了但任务被判定 UP-TO-DATE 会留下过期报告，比门禁空转更糟。
	val versionReport = layout.buildDirectory.file(versionReportPath)
	val versionCoherenceCheck = tasks.register("versionCoherenceCheck") {
		group = "verification"
		description = "Assert $sameVersionGroups is version-coherent in this module and write the resolved-version report"
		val resolved = provider {
			coherenceConfigurations.mapNotNull { name ->
				val cfg = configurations.findByName(name)
				if (cfg == null || !cfg.isCanBeResolved) {
					null
				} else {
					cfg.incoming.artifacts.artifacts.mapNotNull { artifact ->
						val id = artifact.id.componentIdentifier as? ModuleComponentIdentifier ?: return@mapNotNull null
						Triple(name, id.displayName.removeSuffix(":${id.version}"), id.version)
					}
				}
			}.flatten().distinct()
		}
		doLast {
			val rows = resolved.get()
			val split = rows.filter { it.second.substringBefore(':') in sameVersionGroups }
				.groupBy({ it.first }, { it })
				.mapNotNull { (name, hits) ->
					if (hits.map { it.third }.distinct().size < 2) null else {
						"  $name ${hits.first().second.substringBefore(':')} -> " +
							hits.sortedBy { it.second }.joinToString(", ") { "${it.second.substringAfter(':')}:${it.third}" }
					}
				}
			if (split.isNotEmpty()) {
				throw GradleException(
					"${project.name}: same-group artifacts resolved to multiple versions\n${split.joinToString("\n")}" +
						"\nA single pinned constraint must be aligned with its BOM-managed siblings; Dependabot only bumps the pinned one.",
				)
			}
			versionReport.get().asFile.apply {
				parentFile.mkdirs()
				writeText(rows.joinToString("\n") { "${it.first}|${it.second}|${it.third}" } + "\n")
			}
		}
	}
	tasks.named("check") {
		dependsOn(versionCoherenceCheck)
	}

	// 覆盖率报告：只出 XML + HTML，不设阈值门禁；数据由 test 任务产出
	tasks.withType<JacocoReport>().configureEach {
		reports {
			xml.required.set(true)
			html.required.set(true)
		}
	}
	tasks.named("test") { finalizedBy("jacocoTestReport") }
}

// pre-push 触发的增量模式：lintMarkdown 只查相对比较基变更的 md（Spotless 的增量见下方注释，已被否决）
val prePushInvoked = gradle.startParameter.taskNames.map { it.substringAfterLast(':') }.contains("prePushCheck")

// Markdown 门禁的覆盖面是白名单，不是 `**/*.md` 减去一串排除：私有工作区目录只增不减，减法名单每多一个
// 就要再红一次提交。这里列受控文档的全部位置——根、每个模块的 README、.github 模板、docs/ 全树。
// 白名单的代价是新位置的文档会静默逃过门禁，由下方 trackedMarkdownOutsideTargets() 兜底成显式失败。
val markdownTargets = listOf("*.md", "*/README.md", ".github/*.md", "docs/**/*.md")

// yaml 与 gradleScripts 的 target 同样列受控位置，而不是从仓库根写 `**/*`：裸 `**/*` 的输入快照与命中文件数
// 无关，Gradle 为判定"无变更"要枚举整棵树（实测见下方 spotless 块内注释），锚定后 include 侧才走目录剪枝。
val yamlTargets = listOf(
	".github/**/*.yml",
	".github/**/*.yaml",
	"docker/**/*.yml",
	"docker/**/*.yaml",
	"*/src/**/*.yml",
	"*/src/**/*.yaml",
)
val gradleScriptTargets = listOf("*.gradle.kts", "*/build.gradle.kts", "gradle/*.gradle.kts")

spotless {
	// 不启用 ratchetFrom：当时 pre-push 的耗时全在三个仓库级 `**/*` target 的输入快照上，与待格式化
	// 文件数无关——Gradle 为判定"无变更"要枚举整棵树（31394 个文件里有 29988 个在 web/node_modules；
	// 15 个文件的 yaml 8m31s 比 181 个文件的 java 3m47s 还慢，而白名单形状的 markdown 只要 1s；
	// `targetExclude` 也从不剪枝，yaml 已排除 node_modules 仍最慢。ratchet 缩输入集合对这笔开销收益为零，
	// 却让输入随分支漂移、跨分支不再复用缓存条目，故当时否决（不带 40m29s、带 34m26s）。
	// 这笔开销已随下面的锚定 target 消掉（java 实测 3m47s → 12.8s），格式检查现在是秒级，更没有 ratchet 的位置。
	java {
		// 锚定后 include 侧才走 Gradle 的目录剪枝；同样不写 targetExclude，理由同下面 markdown 那条
		target("*/src/**/*.java")
		palantirJavaFormat("2.97.0")
		removeUnusedImports()
		toggleOffOn()
	}
	format("markdown") {
		// 白名单里没有 node_modules、build 与各私有工作区目录，它们不需要再被逐个排除；
		// 也刻意不写 targetExclude：Spotless 把它实现成 target.minus(targetExclude)（两边各自解析成文件树），
		// 排除侧只会多走一遍全树，从不剪枝
		target(markdownTargets)
		prettier()
			.config(
				mapOf(
					"printWidth" to 120,
					"tabWidth" to 2,
					"useTabs" to false,
					"singleQuote" to true,
					"proseWrap" to "preserve",
					"trailingComma" to "none",
				),
			)
		trimTrailingWhitespace()
	}
	// 只定空白与文件结尾，不引入 ktlint：避免把既有 tab 缩进的构建脚本整体重排
	format("gradleScripts") {
		target(gradleScriptTargets)
		trimTrailingWhitespace()
		endWithNewline()
	}
	format("yaml") {
		// web/pnpm-lock.yaml 不在 yamlTargets 里：lockfile 由包管理器生成，Prettier 重排会让每次 install 产生无意义 churn
		target(yamlTargets)
		prettier()
		trimTrailingWhitespace()
		endWithNewline()
	}
	// 刻意不格式化 db/migration/*.sql：Flyway 脚本是历史记录，重排会让迁移 diff 无法审查
}

val isWindows = System.getProperty("os.name").lowercase().contains("windows")
val npx = if (isWindows) "npx.cmd" else "npx"

// 白名单把「每次提交都红」换成「新位置的文档静默逃过门禁」，所以要用 git 的跟踪清单兜底：
// 被跟踪的 *.md 只要落不进 markdownTargets 就显式失败。下面的判定与 markdownTargets 逐条对齐，改一处必须同步另一处。
fun escapesMarkdownTargets(path: String): Boolean {
	val parts = path.split('/')
	return when {
		parts.size == 1 -> false // *.md
		parts.size == 2 && (parts[0] == ".github" || parts[1] == "README.md") -> false // .github/*.md、*/README.md
		parts[0] == "docs" -> false // docs/**/*.md：** 匹配零层目录，docs/ 直属文件同样覆盖
		else -> true
	}
}

// .git 不可用时（源码归档、沙箱）返回 null，跳过这条检查，与 changedMarkdownSince 的处理一致
fun trackedMarkdownOutsideTargets(): List<String>? = try {
	val process = ProcessBuilder("git", "ls-files", "-z", "--", "*.md").directory(rootDir).start()
	val paths = process.inputStream.bufferedReader().readText().split('\u0000')
	if (process.waitFor() != 0) null else paths.filter { it.isNotBlank() && escapesMarkdownTargets(it) }
} catch (_: Exception) {
	null
}

// pre-push 是增量场景：只查相对比较基（默认 origin/main 合并基）变更的 Markdown，避免每次推送
// 都为全仓 md 付一遍 npx 冷启动；check/CI 仍是全量。-PmdBase=<ref> 可覆盖比较基。
// --diff-filter=ACMR 排除删除的 md（文件已不在，markdownlint 会因路径不存在直接报错）。
// git 不可用或解析不出比较基时返回 null，回退全量
fun changedMarkdownSince(base: String): List<String>? = try {
	val process = ProcessBuilder("git", "diff", "--name-only", "--diff-filter=ACMR", "$base...HEAD", "--", "*.md")
		.directory(rootDir)
		.start()
	val names = process.inputStream.bufferedReader().readText()
	if (process.waitFor() != 0) null else names.trim().lines().filter { it.isNotBlank() }
} catch (_: Exception) {
	null
}

val mdBase = providers.gradleProperty("mdBase")
val lintIncrementally = mdBase.isPresent || prePushInvoked
val changedMarkdown = if (lintIncrementally) changedMarkdownSince(mdBase.getOrElse("origin/main")) else null

val lintMarkdown = tasks.register<Exec>("lintMarkdown") {
	group = "verification"
	description = "Check Markdown rules (full repo by default; only changed files under prePushCheck or -PmdBase)"
	commandLine(listOf(npx, "markdownlint-cli") + markdownTargets)
	doFirst {
		val outside = trackedMarkdownOutsideTargets()
		if (outside != null && outside.isNotEmpty()) {
			throw GradleException(
				"tracked Markdown sits outside the allowlist, so it would silently skip the format and lint gate:\n" +
					outside.sorted().joinToString("\n") { "  $it" } +
					"\nList its location in markdownTargets (build.gradle.kts), or move the document under docs/.",
			)
		}
	}
	changedMarkdown?.let { files ->
		val base = mdBase.getOrElse("origin/main")
		onlyIf("no Markdown changes vs $base") { files.isNotEmpty() }
		if (files.isNotEmpty()) {
			doFirst {
				commandLine = listOf(npx, "markdownlint-cli") + files
			}
		}
	}
}

val lintMarkdownFix = tasks.register<Exec>("lintMarkdownFix") {
	group = "verification"
	description = "Auto-fix Markdown rule violations (markdownlint --fix)"
	commandLine(listOf(npx, "markdownlint-cli") + markdownTargets + listOf("--fix"))
}

// TaskProvider 懒加载，避免配置期急切解析所有子项目
val compileGate = subprojects.flatMap { subproject ->
	listOf(
		subproject.tasks.named("testClasses"),
		subproject.tasks.named("spotbugsMain"),
		subproject.tasks.named("architectureTest"),
		subproject.tasks.named("versionCoherenceCheck"),
	)
}

// 同一坐标在各模块必须解析到同一版本。common-core 的 testRuntimeClasspath 曾在 Jackson 2.12.7.1 上跑，
// 而 admin-service 运行时是 2.21.5——库模块自测的版本和部署不是一套，CI 绿着也藏得住。
// 根因是托管优先级：spring-cloud-alibaba-dependencies 直接声明的坐标只在没挂 Boot 插件的模块里生效。
// 数据来自各模块 versionCoherenceCheck 写的报告（并行执行下根任务不能直接解析别人的配置）。
val crossModuleVersionCheck = tasks.register("crossModuleVersionCheck") {
	group = "verification"
	description = "Compare resolved-version reports across modules; fail if one coordinate has multiple versions"
	dependsOn(subprojects.map { it.tasks.named("versionCoherenceCheck") })
	val reports = subprojects.map { it.name to it.layout.buildDirectory.file(versionReportPath) }
	doLast {
		// key = "配置 坐标"，value = 各模块解析到的版本
		val seen = mutableMapOf<String, MutableList<Pair<String, String>>>()
		reports.forEach { (moduleName, report) ->
			val file = report.get().asFile
			if (!file.isFile) {
				throw GradleException("missing resolved-version report for $moduleName: $file. Run ./gradlew versionCoherenceCheck first")
			}
			file.readLines().filter { it.isNotBlank() }.forEach { line ->
				val (configurationName, coordinate, version) = line.split('|')
				seen.getOrPut("$configurationName $coordinate") { mutableListOf() } += moduleName to version
			}
		}
		val drift = seen.toList()
			.filter { it.second.map { (_, version) -> version }.distinct().size > 1 }
			.sortedBy { it.first }
			.map { (key, hits) ->
				"  $key -> " + hits.sortedBy { it.first }.joinToString(", ") { (moduleName, version) -> "$moduleName=$version" }
			}
		if (drift.isNotEmpty()) {
			throw GradleException("the same coordinate resolved to different versions in different modules:\n${drift.joinToString("\n")}")
		}
	}
}

// 推送前轻量门禁：格式 + Markdown 规则 + 编译零告警 + SpotBugs + 架构约束 + 版本一致性，不跑测试；全量门禁仍是 check
val prePushCheck = tasks.register("prePushCheck") {
	group = "verification"
	description = "Pre-push gate (Markdown check is incremental): format + Markdown rules + compile (-Werror) + SpotBugs + architecture + version coherence"
	dependsOn(tasks.named("spotlessCheck"), lintMarkdown)
	dependsOn(compileGate)
	dependsOn(crossModuleVersionCheck)
}

tasks.named("spotlessApply") {
	finalizedBy(lintMarkdownFix)
}

tasks.named("check") {
	dependsOn(lintMarkdown, crossModuleVersionCheck)
}

// 本机开发栈编排（devUp / devDown / devStatus / devComposeDown）：逻辑独立成文件，根脚本不长；
// 只被开发者手动调用，不进 check 也不进 prePushCheck
apply(from = "gradle/dev-orchestration.gradle.kts")
