plugins {
    `java-library`
}

group = "org.zeniot.common"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":common:util"))
    implementation(project(":common:data"))
    api(libs.netty.all)

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
