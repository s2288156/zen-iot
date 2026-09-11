#!/usr/bin/env kotlin

plugins {
	id("org.danilopianini.gradle-pre-commit-git-hooks") version "2.1.23"
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
