plugins {
    idea
    id("org.springframework.boot").version("3.2.2")
    id("io.spring.dependency-management").version("1.1.0")
}

allprojects {
    group = "org.zeniot"
    version = "0.0.1"

    apply(plugin = "idea")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
}
