plugins {
	java
	id("io.spring.dependency-management") version "1.1.7"
	id("com.diffplug.spotless") version "8.10.1"
}

group = "com.zen"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
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

spotless {
	java {
		target("**/*.java")
		targetExclude("**/build/**")
		googleJavaFormat("1.36.1")
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

val lintMarkdown = tasks.register<Exec>("lintMarkdown") {
	group = "verification"
	description = "检查 Markdown 规范"
	commandLine(npx, "markdownlint", "**/*.md")
}

tasks.named("check") {
	dependsOn(lintMarkdown)
}
