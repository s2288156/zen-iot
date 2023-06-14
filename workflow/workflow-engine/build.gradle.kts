plugins {
    `java-library`
}

dependencies {
    implementation(project(":common:util"))
    implementation(project(":common:data"))
    api(project(":workflow:workflow-api"))
    api(project(":dao"))

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
}
