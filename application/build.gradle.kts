plugins {
    `java-library`
}

dependencies {
    implementation(project(":api"))
//    implementation(project(":common:transport-api"))
    implementation(project(":common:util"))
    implementation(project(":common:data"))
    api(libs.nimbus.jose.jwt)
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.boot:spring-boot-starter-actuator")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
}
