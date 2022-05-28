plugins {
    `java-library`
}

dependencies {
    api("org.springframework.boot:spring-boot-starter-json")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}