plugins {
	id("org.springframework.boot")
}

dependencies {
	// 网关只要 common-core 的 JWT 校验、统一响应与错误码。common-core 以 api 暴露的三个 starter 必须逐条排掉：
	// - spring-boot-starter-web：上类路径后 Boot 把应用判成 Servlet 类型，WebFlux 网关启动即失败；
	// - spring-boot-starter-data-jpa：无数据源时 DataSourceAutoConfiguration 直接炸，且 ZenJpaAuditingAutoConfiguration
	//   是 matchIfMissing = true（缺省即开），会跟着注册 @EnableJpaAuditing；
	// - spring-boot-starter-validation：网关不校验入参 DTO，用不到。
	// 排完仍留在运行时类路径上的是 tracing bridge（P3-1 的 A 层：只生成/透传 traceId，span 不上报），这是本阶段要的。
	// 类路径隔离由 GatewayDependencyIsolationTest 与 ArchitectureTest 共同把守。
	// Phase 2「关键决策」推荐的长期正解是把 JWT 拆进无 Web 依赖的 common-security；本阶段先按备选走 exclude，见
	// .ai/dev-plan/实施进度.md 的 Phase 2 未收口事项。
	implementation(project(":common-core")) {
		exclude(group = "org.springframework.boot", module = "spring-boot-starter-web")
		exclude(group = "org.springframework.boot", module = "spring-boot-starter-validation")
		exclude(group = "org.springframework.boot", module = "spring-boot-starter-data-jpa")
	}
	// Cloud 2025.1 起网关拆成 webflux / webmvc 两套，反应式的是 -server-webflux 这个坐标（由 Cloud BOM 管到 5.0.3）
	implementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webflux")
	// lb://{service} 需要客户端负载均衡；网关 starter 不传递该 starter，缺它 lb:// 解析不出实例
	implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")
	// 反应式 Redis：Netty EventLoop 上用命令式 StringRedisTemplate 做 hasKey，高并发下静默堵死事件循环且不报错
	implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	// 只要 /actuator/prometheus 端点就绪（与 admin-service 同口径），Prometheus 服务端在 Phase 10 部署；
	// 缺它时 Boot 只会静默少暴露一个端点，日志里表现为「Exposing 1 endpoint」
	runtimeOnly("io.micrometer:micrometer-registry-prometheus")
	implementation("com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery")
	// 与 admin-service 同一份 jjwt：common-core 只以 api 暴露 jjwt-api，实现与序列化器要自己带上
	runtimeOnly("io.jsonwebtoken:jjwt-impl")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson")

	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.projectreactor:reactor-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.bootJar {
	archiveFileName.set("gateway-service.jar")
}
