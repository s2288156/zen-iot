plugins {
	id("java-library")
	id("io.spring.dependency-management")
	// 挂 Boot 插件只为拿它注入的托管版本。只挂依赖管理插件时，spring-cloud-alibaba-dependencies
	// 直接声明的坐标会盖过 Boot 的 BOM（实测 jackson-databind 2.12.7.1、lombok 1.18.38、slf4j-api 2.0.17），
	// 于是本模块自测用的版本和 admin-service 运行时不是一套；挂上之后两模块 2230 项托管版本全等。
	id("org.springframework.boot")
}

// Boot 插件不会因为是 java-library 就自动关 bootJar，不关则 assemble 报「Main class name has not been
// configured」；关掉之后要重新启用 jar，并把 Boot 给 jar 的 -plain classifier 复位成默认产物名
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") { enabled = false }
tasks.named<Jar>("jar") {
	enabled = true
	archiveClassifier.set("")
}

dependencies {
	// 类型出现在公共签名中的 starter 必须以 api 暴露
	api("org.springframework.boot:spring-boot-starter-web")
	api("org.springframework.boot:spring-boot-starter-validation")
	api("org.springframework.boot:spring-boot-starter-data-jpa")
	// 链路追踪只做 A 层：traceId 生成/透传/写 MDC，日志可串联，但 span 不上报。
	// 刻意不用 spring-boot-starter-opentelemetry——它连带 opentelemetry-exporter-otlp 与
	// micrometer-registry-otlp，会真的开始外发数据；Phase 10 部署追踪后端时再换成该 starter。
	// Boot 4 起自动配置独立成模块，不引这个就没有 Tracer/traceId 装配
	implementation("org.springframework.boot:spring-boot-micrometer-tracing-opentelemetry")
	runtimeOnly("io.micrometer:micrometer-tracing-bridge-otel")
	// JWT 工具与 Phase 2 的 WebFlux 网关复用同一份代码,故 com.zen.common.core.jwt 不得引用 Servlet API
	api("io.jsonwebtoken:jjwt-api")
	runtimeOnly("io.jsonwebtoken:jjwt-impl")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson")

	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
