plugins {
    `java-library`
}
dependencies {
    api(project(":common:util"))
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("org.springframework.boot:spring-boot-starter-validation")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.flywaydb:flyway-core")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}