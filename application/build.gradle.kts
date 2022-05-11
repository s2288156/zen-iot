
dependencies {
    implementation(project(":dao"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
