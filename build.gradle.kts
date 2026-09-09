plugins {
	java
	id("io.spring.dependency-management") version "1.1.7" apply false
	id("org.springframework.boot") version "4.1.1" apply false
	id("com.diffplug.spotless") version "8.10.1"
}

group = "com.zen"
version = "0.0.1-SNAPSHOT"

subprojects {
	apply(plugin = "java")
	apply(plugin = "io.spring.dependency-management")

	// 版本只在根声明一次，子模块依赖一律不写版本号；接入新的第三方栈时在此追加 BOM
	configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
		imports {
			mavenBom("org.springframework.boot:spring-boot-dependencies:4.1.1")
			mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.1.2")
			mavenBom("com.alibaba.cloud:spring-cloud-alibaba-dependencies:2025.1.0.0")
			// common-core 以 api 暴露 jjwt 类型，版本必须在根统一管；模块局部 BOM 不会传递给消费方
			mavenBom("io.jsonwebtoken:jjwt-bom:0.12.7")
		}
	}

	group = "com.zen"
	version = "0.0.1-SNAPSHOT"

	configure<JavaPluginExtension> {
		toolchain {
			languageVersion.set(JavaLanguageVersion.of(21))
		}
	}

	repositories {
		maven { url = uri("https://maven.aliyun.com/repository/public") }
		maven { url = uri("https://maven.aliyun.com/repository/central") }
		mavenLocal()
		mavenCentral()
	}

	tasks.withType<Test> {
		useJUnitPlatform()
	}
}

spotless {
	java {
		target("**/*.java")
		targetExclude("**/build/**")
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

// 推送前轻量门禁：只校格式与编译，不跑依赖本机 MySQL/Nacos 的测试；全量门禁仍是 check
val prePushCheck = tasks.register("prePushCheck") {
	group = "verification"
	description = "推送前轻量门禁：Java/Markdown 格式 + Markdown 规范 + 全模块编译（不跑测试）"
	dependsOn(tasks.named("spotlessCheck"), lintMarkdown)
	dependsOn(subprojects.map { "${it.path}:testClasses" })
}

tasks.named("spotlessApply") {
	finalizedBy(lintMarkdownFix)
}

tasks.named("check") {
	dependsOn(lintMarkdown)
}
