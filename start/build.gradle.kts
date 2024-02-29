plugins {
    id("java")
    id("org.graalvm.buildtools.native") version "0.9.18"
}

java.sourceCompatibility = JavaVersion.VERSION_21
java.targetCompatibility = JavaVersion.VERSION_21

dependencies {
    implementation(project(":dao"))
    implementation(project(":service"))
    implementation(project(":application"))

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
