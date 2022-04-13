
dependencies {
    implementation(project(":dao"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
}

tasks {
    test {
        useJUnitPlatform()
    }
}
