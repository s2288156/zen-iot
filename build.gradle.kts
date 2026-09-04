plugins {
	java
	id("org.springframework.boot") version "4.1.1"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.zen"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	maven { url = uri("https://maven.aliyun.com/repository/public") }
	maven { url = uri("https://maven.aliyun.com/repository/central") }
	mavenLocal()
	mavenCentral()
}

tasks.withType<Test> {
	useJUnitPlatform()
}
