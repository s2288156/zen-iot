plugins {
    `java-library`
}

dependencies {
    api(project(":dao"))
    api(project(":api"))
    api(project(":common:transport-api"))
    implementation(project(":common:mqtt"))
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    testRuntimeOnly("com.h2database:h2")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
