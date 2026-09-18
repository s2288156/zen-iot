#!/usr/bin/env kotlin

// 插件版本一律硬编码：本文件只有 git-hooks 一个插件，根 build.gradle.kts 的 4 个插件同理
// 未写 pluginManagement 块：目前没有自定义解析源的需求，真要接私有插件仓库时在此声明

plugins {
	id("org.danilopianini.gradle-pre-commit-git-hooks") version "2.1.24"
}

// 集中式仓库声明：根项目（Spotless detached configuration）与所有子项目自动继承；
// 子项目不再单独声明 repositories，若未来某个子项目需要私有仓库可在其脚本里追加
dependencyResolutionManagement {
	repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
	repositories {
		maven { url = uri("https://maven.aliyun.com/repository/public") }
		mavenCentral()
		mavenLocal()
	}
}

// .git 不可写（agent 沙箱、源码包）时跳过钩子安装：./gradlew <task> -PskipGitHooks
val installGitHooks = File(rootDir, ".git").exists() && !startParameter.projectProperties.containsKey("skipGitHooks")

gitHooks {
	preCommit {
		tasks("spotlessCheck")
	}
	commitMsg {
		// 不用插件的 conventionalCommits() 便捷方法：它的内置脚本用 POSIX [[:graph:]] / [[:alnum:]]，
		// 在 GNU grep 默认 locale 下只匹配 ASCII，导致中文 subject / 含中文 scope 的提交信息被误拒。
		// 下方独立脚本完全照搬插件模板（v2.1.24），仅放宽 scope/subject 两处字符类以支持 CJK。
		from(file("gradle/git-hooks/commit-msg.sh"))
	}
	hook("pre-push") {
		tasks("prePushCheck")
	}
	// true = 每次调用都按本脚本覆盖生成，钩子文件不接受本地手改
	if (installGitHooks) {
		createHooks(true)
	}
}

rootProject.name = "zen-iot"

include("admin-service")
include("common-core")
include("common-security")
include("gateway-service")
