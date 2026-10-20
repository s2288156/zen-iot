plugins {
	id("org.springframework.boot")
}

dependencies {
	implementation(project(":common-core"))
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	// Boot 4 起 Flyway 自动配置独立成 starter；MySQL 方言需 flyway-mysql
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.flywaydb:flyway-mysql")
	// 网关心跳与健康检查用；注册中心见 application.yml 的 spring.cloud.nacos
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	// 只保证 /actuator/prometheus 端点与 exposition 格式就绪，Prometheus/Grafana 服务端在 Phase 10 部署
	runtimeOnly("io.micrometer:micrometer-registry-prometheus")
	implementation("com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery")
	// 只要 BCrypt 编码器；引入 starter-security 会装配默认过滤器链，接管 401/403 并绕过统一异常处理
	implementation("org.springframework.security:spring-security-crypto")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui")

	runtimeOnly("com.mysql:mysql-connector-j")

	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.bootJar {
	archiveFileName.set("admin-service.jar")
}

// OpenAPI 的 info.version 由构建注入：bootBuildInfo 产出的 META-INF/build-info.properties 被挂成 main resources
// 的 srcDir，所以测试、bootRun 与 bootJar 里都在（见 ZenAdminOpenApiConfiguration 的构造参数）。
// excludes 掉 time：它默认是「当前时刻」、本身又是任务的 @Input，留着会让 bootBuildInfo 与随后的
// processResources 每次构建都失稳；本项目只读 build.version。第二个产出对外文档的模块照抄这一段。
springBoot {
	buildInfo {
		excludes.set(setOf("time"))
	}
}
