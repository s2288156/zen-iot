plugins {
    `java-library`
}

dependencies {
    implementation(project(":common:util"))
    api("org.springframework.boot:spring-boot-starter-validation")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
}
