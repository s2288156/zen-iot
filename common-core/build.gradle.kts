plugins {
	id("java-library")
	id("io.spring.dependency-management")
}

// Spring Framework 7 移除了无 -parameters 时的降级参数名发现;Boot 插件会自动加该 flag，本模块未 apply Boot 插件需显式声明
tasks.withType<JavaCompile> {
	options.compilerArgs.add("-parameters")
}

dependencies {
	// 类型出现在公共签名中的 starter 必须以 api 暴露
	api("org.springframework.boot:spring-boot-starter-web")
	api("org.springframework.boot:spring-boot-starter-validation")
	api("org.springframework.boot:spring-boot-starter-data-jpa")
	// JWT 工具与 Phase 2 的 WebFlux 网关复用同一份代码,故 com.zen.common.core.jwt 不得引用 Servlet API
	api("io.jsonwebtoken:jjwt-api")
	runtimeOnly("io.jsonwebtoken:jjwt-impl")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson")

	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
