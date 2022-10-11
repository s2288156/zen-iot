plugins {
    `java-library`
}

dependencies {
    api(project(":dao"))
    api(project(":api"))
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
