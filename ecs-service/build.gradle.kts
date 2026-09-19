plugins {
	id("org.springframework.boot")
}

dependencies {
	// 只声明 common-core：它以 api 透传 common-security（JWT / ApiResponse / 错误码 / TrustedHeaders），
	// 在这里再写一遍 common-security 属于重复声明，会让跨模块版本比对的输入变模糊。
	implementation(project(":common-core"))
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	// 心跳在线状态与超时计时放 Redis：设备秒级上报不该进 MySQL，且键 TTL 天然是「最近一次心跳」的载体
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	// 指令中转：消费 RCS 下发的搬运指令（Phase 9 定全链路拓扑，本阶段只落 ECS 这一侧的消费者）
	implementation("org.springframework.boot:spring-boot-starter-amqp")
	// Boot 4 起 Flyway 自动配置独立成 starter；MySQL 方言需 flyway-mysql
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.flywaydb:flyway-mysql")
	// 网关心跳与健康检查用；注册中心见 application.yml 的 spring.cloud.nacos
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	// 只要 /actuator/prometheus 端点与 exposition 格式就绪，Prometheus 服务端在 Phase 10 部署
	runtimeOnly("io.micrometer:micrometer-registry-prometheus")
	implementation("com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery")

	runtimeOnly("com.mysql:mysql-connector-j")

	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.bootJar {
	archiveFileName.set("ecs-service.jar")
}
