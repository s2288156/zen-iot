/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    id("org.zeniot.java-application-conventions")
}

dependencies {
    implementation("org.apache.commons:commons-text")
    implementation(project(":utilities"))
}

application {
    // Define the main class for the application.
    mainClass.set("org.zeniot.app.App")
}
