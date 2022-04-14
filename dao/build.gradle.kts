plugins {
    `java-library`
}
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")
//    implementation("org.flywaydb:flyway-core")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}