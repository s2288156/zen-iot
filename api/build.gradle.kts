plugins {
    `java-library`
}

dependencies {
    implementation(project(":dao"))
    api(project(":common:data"))
    api("org.springframework.boot:spring-boot-starter-validation")
    api("org.springframework.boot:spring-boot-starter-security")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
}
