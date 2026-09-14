#!/usr/bin/env kotlin

// settings.kts 只有一个 git-hooks 插件，硬编码可接受；根 build.gradle.kts 的 4 个插件走 catalog alias
// pluginManagement 块在此省略：Catalog 自动加载只对 build.gradle.kts 生效，settings 若需 alias 需显式声明

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
		conventionalCommits()
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
