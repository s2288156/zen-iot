#!/usr/bin/env kotlin

plugins {
	id("org.danilopianini.gradle-pre-commit-git-hooks") version "2.1.23"
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
