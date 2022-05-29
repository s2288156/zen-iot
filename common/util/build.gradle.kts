plugins {
    `java-library`
}

dependencies {
    api("org.apache.commons:commons-lang3")
    api("org.springframework.boot:spring-boot-starter-json")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
