plugins {
    java
}

dependencies {
    implementation(project(":dao"))
    implementation(project(":common:mqtt"))
    implementation(libs.nimbus.jose.jwt)
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    testRuntimeOnly("com.h2database:h2")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

//tasks.compileJava {
//    options.release.set(17)
//}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
