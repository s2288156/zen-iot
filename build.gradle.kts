plugins {
	java
	id("io.spring.dependency-management") version "1.1.7" apply false
	id("org.springframework.boot") version "4.1.1" apply false
	id("com.diffplug.spotless") version "8.10.1"
	id("com.github.spotbugs") version "6.5.11" apply false
}

group = "com.zen"
version = "0.0.1-SNAPSHOT"

// 依赖本机 MySQL/Nacos/Redis 的测试一律打 @Tag("integration")，默认从 test/check 排除，
// 这样 check 在干净环境下也是可执行的真门禁；容器就绪时用 ./gradlew test -PintegrationTests 纳入
val integrationTag = "integration"
val runIntegrationTests = project.hasProperty("integrationTests")

subprojects {
	apply(plugin = "java")
	apply(plugin = "io.spring.dependency-management")
	apply(plugin = "com.github.spotbugs")

	// 版本只在根声明一次，子模块依赖一律不写版本号；接入新的第三方栈时在此追加 BOM
	configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
		imports {
			mavenBom("org.springframework.boot:spring-boot-dependencies:4.1.1")
			mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.1.2")
			mavenBom("com.alibaba.cloud:spring-cloud-alibaba-dependencies:2025.1.0.0")
			// common-core 以 api 暴露 jjwt 类型，版本必须在根统一管；模块局部 BOM 不会传递给消费方
			mavenBom("io.jsonwebtoken:jjwt-bom:0.12.7")
		}
		// ArchUnit 没有 BOM，用单条约束把版本留在根，模块仍不写版本号
		dependencies {
			dependency("com.tngtech.archunit:archunit-junit5:1.5.0")
			/*
			 * Boot 4.1.1 的 BOM 把 log4j2 钉在 2.25.1，而 2.25.1 的 POM 链里
			 * error_prone_annotations 的版本是 ${error-prone.version}——该属性定义在上溯两级的
			 * logging-parent 里，Gradle 的 POM 模型构建器不展开它，于是每次解析都刷一条
			 * 「Errors occurred while building effective model」告警（仅告警，不影响构建结果）。
			 * 全构建唯一带 log4j-core 的地方是 SpotBugs 引擎的 classpath（spotbugs 配置），
			 * 依赖管理插件对该配置一视同仁地改写版本（force / strictly 实测都被它压过）。
			 * 这里按应用侧 nacos-log4j2-adapter 实际解析到的 2.25.5 对齐：2.25.1 不再进图，告警消失，
			 * 且 log4j-core 与 log4j-api 保持同版本。Boot 的 BOM 升到 ≥ 2.26.1 后可删掉这条。
			 */
			dependency("org.apache.logging.log4j:log4j-core:2.25.5")
		}
	}

	group = "com.zen"
	version = "0.0.1-SNAPSHOT"

	configure<JavaPluginExtension> {
		toolchain {
			languageVersion.set(JavaLanguageVersion.of(21))
		}
	}

	dependencies {
		testImplementation("com.tngtech.archunit:archunit-junit5")
	}

	repositories {
		maven { url = uri("https://maven.aliyun.com/repository/public") }
		mavenCentral()
		mavenLocal()
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
		targetExclude("**/build/**", "**/.gradle/**", "**/node_modules/**")
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

// markdownlint-cli 的 `**` 不匹配以点开头的路径段，需显式列出隐藏的 .ai 目录
val markdownTargets = listOf("**/*.md", ".ai/**/*.md")

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
	)
}

// 推送前轻量门禁：格式 + 编译零告警 + 架构约束 + SpotBugs 缺陷扫描，不跑测试；全量门禁仍是 check
val prePushCheck = tasks.register("prePushCheck") {
	group = "verification"
	description = "推送前门禁：Java/Markdown/kts/YAML 格式 + Markdown 规范 + 编译(-Werror) + SpotBugs(main) + 架构约束"
	dependsOn(tasks.named("spotlessCheck"), lintMarkdown)
	dependsOn(compileGate)
}

tasks.named("spotlessApply") {
	finalizedBy(lintMarkdownFix)
}

tasks.named("check") {
	dependsOn(lintMarkdown)
}
