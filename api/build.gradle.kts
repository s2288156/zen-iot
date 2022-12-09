plugins {
    `java-library`
}

dependencies {
    implementation(project(":dao"))
    api(project(":common:data"))
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
}
