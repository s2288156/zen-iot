plugins {
	id("java-library")
	id("io.spring.dependency-management")
	// 与 common-core 同理：库模块必须挂 Boot 插件才拿得到托管版本（只挂依赖管理插件时 SCA 直接声明的坐标会盖过 Boot 的 BOM），
	// 代价是要显式关掉 bootJar 并把 jar 的 classifier 复位
	id("org.springframework.boot")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") { enabled = false }
tasks.named<Jar>("jar") {
	enabled = true
	archiveClassifier.set("")
}

dependencies {
	// 本模块存在的理由：只放 Web 无关的安全内核（JWT、统一响应契约、错误码、身份头与撤销契约），
	// 因此刻意不引 spring-boot-starter-web / -validation / -data-jpa——反应式网关直接依赖它就不必再逐条排依赖。
	// 这条不变量由 common-security 的 ArchitectureTest.moduleStaysFreeOfWebAndPersistence() 把守，
	// 网关侧另有 GatewayDependencyIsolationTest 从消费方视角复核。
	api("org.springframework.boot:spring-boot-starter")
	// ApiResponse 上的 @JsonInclude 出现在公共签名里
	api("com.fasterxml.jackson.core:jackson-annotations")
	// 类型出现在公共签名中的 jjwt-api 必须以 api 暴露，版本由根的 jjwt-bom 统一管
	api("io.jsonwebtoken:jjwt-api")
	runtimeOnly("io.jsonwebtoken:jjwt-impl")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson")

	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
