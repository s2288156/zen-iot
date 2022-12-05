plugins {
    id("java")
    id("org.graalvm.buildtools.native") version "0.9.18"
}

dependencies {
    implementation(project(":dao"))
    implementation(project(":service"))
    implementation(project(":application"))

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    testRuntimeOnly("com.h2database:h2")
}

java.sourceCompatibility = JavaVersion.VERSION_17

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
