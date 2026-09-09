plugins {
	id("org.springframework.boot")
}

dependencies {
	implementation(project(":common-core"))
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	// 只要 BCrypt 编码器；引入 starter-security 会装配默认过滤器链，接管 401/403 并绕过统一异常处理
	implementation("org.springframework.security:spring-security-crypto")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.0")

	runtimeOnly("com.mysql:mysql-connector-j")

	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.bootJar {
	archiveFileName.set("admin-service.jar")
}
