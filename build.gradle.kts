/*
 * This file was generated by the Gradle 'init' task.
 *
 * This is a general purpose Gradle build.
 * Learn more about Gradle by exploring our samples at https://docs.gradle.org/7.4.2/samples
 */
plugins {
    idea
    java
    id("org.springframework.boot").version("2.7.1")
    id("io.spring.dependency-management").version("1.0.11.RELEASE")
}

allprojects {
    group = "org.zeniot"
    version = "0.0.1"

    apply(plugin = "idea")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
}

