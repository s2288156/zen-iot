plugins {
    `java-library`
}

dependencies {
    implementation(project(":common:transport-api"))
    implementation("io.netty:netty-all:4.1.110.Final")
//    api(libs.plc4j.api)
//    implementation(libs.plc4j.driver.ads)

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
}

tasks.test {
    useJUnitPlatform()
}
