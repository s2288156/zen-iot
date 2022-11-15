plugins {
    `java-library`
}

dependencies {
    implementation(project(":dao"))
    implementation(libs.mapstruct)
    api(project(":common:data"))
    api("org.springframework.boot:spring-boot-starter-security")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor(libs.mapstruct.processor)
}
