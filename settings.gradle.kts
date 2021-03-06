/*
 * This file was generated by the Gradle 'init' task.
 *
 * The settings file is used to specify which projects to include in your build.
 *
 * Detailed information about configuring a multi-project build in Gradle can be found
 * in the user manual at https://docs.gradle.org/7.4.2/userguide/multi_project_builds.html
 */

rootProject.name = "zen-iot"
include("application")
include("dao")
include("common")
include("common:util")
findProject(":common:util")?.name = "util"
include("app-ui")
