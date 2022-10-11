plugins {
    id("java")
}

dependencies {
    implementation(project(":dao"))
    implementation(project(":service"))
    implementation(project(":application"))

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
