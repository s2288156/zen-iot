plugins {
	java
	id("io.spring.dependency-management") version "1.1.7" apply false
	id("org.springframework.boot") version "4.1.1" apply false
	id("com.diffplug.spotless") version "8.10.2"
	id("com.github.spotbugs") version "6.5.11" apply false
}

group = "com.zen"
version = "0.0.1-SNAPSHOT"

// 依赖本机 MySQL/Nacos/Redis 的测试一律打 @Tag("integration")，默认从 test/check 排除，
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
	configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
		imports {
			mavenBom("org.springframework.boot:spring-boot-dependencies:4.1.1")
			mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.1.3")
			mavenBom("com.alibaba.cloud:spring-cloud-alibaba-dependencies:2025.1.0.0")
			// common-core 以 api 暴露 jjwt 类型，版本必须在根统一管；模块局部 BOM 不会传递给消费方
			mavenBom("io.jsonwebtoken:jjwt-bom:0.13.0")
		}
		// ArchUnit 没有 BOM，用单条约束把版本留在根，模块仍不写版本号
		dependencies {
			dependency("com.tngtech.archunit:archunit-junit5:1.5.0")
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
		description = "架构约束测试（ArchUnit，无需本机中间件）"
		testClassesDirs = testSourceSet.output.classesDirs
		classpath = testSourceSet.runtimeClasspath
		dependsOn(tasks.named("testClasses"))
		useJUnitPlatform {
			includeTags("architecture")
		}
	}

	// 6.5.x 默认不生成任何报告文件，必须显式打开，否则失败时只有一句 exit code 1
	// 插件默认不注册任何报告，失败时只会看到 exit code 1；这里显式建 HTML + XML 两类报告
	tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
		reports.create("html") { required.set(true) }
		reports.create("xml") { required.set(true) }
	}

	// 缺陷扫描：6.5.x 的 Threshold 已改名 Confidence
	configure<com.github.spotbugs.snom.SpotBugsExtension> {
		toolVersion.set("4.10.4")
		effort.set(com.github.spotbugs.snom.Effort.DEFAULT)
		reportLevel.set(com.github.spotbugs.snom.Confidence.MEDIUM)
		ignoreFailures.set(false)
		excludeFilter.set(rootProject.layout.projectDirectory.file("gradle/spotbugs/exclude.xml"))
	}

	// 只解析本模块的配置：并行执行下跨项目解析会被 Gradle 拒（attempted without an exclusive lock）。
	// 所以比对分两步——本任务断言"同组同版本"并写出已解析版本报告，根任务 crossModuleVersionCheck 读报告做跨模块比较。
	// 故意不声明 outputs：依赖版本变了但任务被判定 UP-TO-DATE 会留下过期报告，比门禁空转更糟。
	val versionReport = layout.buildDirectory.file(versionReportPath)
	val versionCoherenceCheck = tasks.register("versionCoherenceCheck") {
		group = "verification"
		description = "断言 $sameVersionGroups 在本模块各配置内同版本，并写出已解析版本报告"
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
					"${project.name}: 同组构件解析出多个版本\n${split.joinToString("\n")}" +
						"\n单条钉住的约束要连 BOM 管的兄弟构件一起对齐，Dependabot 只会抬前者。",
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

spotless {
	java {
		target("**/*.java")
		targetExclude("**/build/**", "**/bin/**")
		palantirJavaFormat("2.97.0")
		removeUnusedImports()
		toggleOffOn()
	}
	format("markdown") {
		target("**/*.md")
		// .ai/ 与 .codex/ 是 gitignore 的本地笔记：Spotless 的 ** 会匹配到点开头的路径，未跟踪文件于是反过来卡住每一次提交
		targetExclude("**/build/**", "**/.gradle/**", "**/node_modules/**", "**/.ai/**", "**/.codex/**")
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
		target("**/*.gradle.kts")
		targetExclude("**/build/**", "**/.gradle/**")
		trimTrailingWhitespace()
		endWithNewline()
	}
	format("yaml") {
		target("**/*.yml", "**/*.yaml")
		targetExclude("**/build/**", "**/.gradle/**", "**/bin/**", "**/node_modules/**")
		prettier()
		trimTrailingWhitespace()
		endWithNewline()
	}
	// 刻意不格式化 db/migration/*.sql：Flyway 脚本是历史记录，重排会让迁移 diff 无法审查
}

val isWindows = System.getProperty("os.name").lowercase().contains("windows")
val npx = if (isWindows) "npx.cmd" else "npx"

// .ai/ 是 gitignore 的临时笔记：和 Spotless 一样排除，未跟踪文件不该卡住 pre-push
val markdownTargets = listOf("**/*.md")

val lintMarkdown = tasks.register<Exec>("lintMarkdown") {
	group = "verification"
	description = "检查 Markdown 规范"
	commandLine(listOf(npx, "markdownlint-cli") + markdownTargets)
}

val lintMarkdownFix = tasks.register<Exec>("lintMarkdownFix") {
	group = "verification"
	description = "自动修复 Markdown 规范问题（markdownlint --fix）"
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
	description = "比对各模块已解析版本报告，同一坐标出现多个版本即失败"
	dependsOn(subprojects.map { it.tasks.named("versionCoherenceCheck") })
	val reports = subprojects.map { it.name to it.layout.buildDirectory.file(versionReportPath) }
	doLast {
		// key = "配置 坐标"，value = 各模块解析到的版本
		val seen = mutableMapOf<String, MutableList<Pair<String, String>>>()
		reports.forEach { (moduleName, report) ->
			val file = report.get().asFile
			if (!file.isFile) {
				throw GradleException("缺少 $moduleName 的已解析版本报告：$file。先跑 ./gradlew versionCoherenceCheck")
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
			throw GradleException("同一坐标在不同模块解析到了不同版本：\n${drift.joinToString("\n")}")
		}
	}
}

// 推送前轻量门禁：格式 + 编译零告警 + 架构约束 + SpotBugs 缺陷扫描，不跑测试；全量门禁仍是 check
val prePushCheck = tasks.register("prePushCheck") {
	group = "verification"
	description = "推送前门禁：Java/Markdown/kts/YAML 格式 + Markdown 规范 + 编译(-Werror) + SpotBugs(main) + 架构约束 + 版本一致性"
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
