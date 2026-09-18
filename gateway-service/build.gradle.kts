plugins {
	id("org.springframework.boot")
}

dependencies {
	// 网关只要「Web 无关的安全内核」：JWT 验签、统一响应契约、错误码、身份头与黑名单契约。
	// 拆模块前这里要逐条 exclude common-core 以 api 暴露的 starter-web/-validation/-data-jpa（漏一条网关就起不来），
	// 现在 common-security 本身不带这些依赖，一条 implementation 就够——该事实由
	// common-security 的 ArchitectureTest.moduleStaysFreeOfWebAndPersistence() 与本模块的
	// GatewayDependencyIsolationTest 双向把守。
	// 只引本项目模块、不引 common-core：网关是反应式应用，碰 Servlet 侧那套拦截器/ThreadLocal 身份上下文就是错的，
	// 这条边界由 ArchitectureTest.neverDependsOnCommonCore() 钉住。
	implementation(project(":common-security"))
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
	// P3-1 的 A 层：traceId 生成/透传/写 MDC + 由 SCG 向下游注入 traceparent。拆模块前它随 common-core 的
	// implementation/runtimeOnly 传递进来，现在按「谁运行谁声明」自己带上——刻意不用
	// spring-boot-starter-opentelemetry（它连带 otlp exporter，会真的外发 span；Phase 10 再换）。
	// 少了这两条，网关日志的 trace 列会为空且不注入 traceparent，由 GatewayDependencyIsolationTest 拦下。
	implementation("org.springframework.boot:spring-boot-micrometer-tracing-opentelemetry")
	runtimeOnly("io.micrometer:micrometer-tracing-bridge-otel")
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.projectreactor:reactor-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.bootJar {
	archiveFileName.set("gateway-service.jar")
}
