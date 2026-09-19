package com.zen.ecs;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 冷启动自检：本服务在「只有 MySQL + Redis + RabbitMQ，没有 Nacos」的条件下能否装配起来。
 *
 * <p>它比看起来重要得多，因为这个模块有两条**只在启动期才暴露**的失败：
 * {@code zen.security.context-source: gateway-header} 没配（jwt 分支拿不到 {@code JwtTokenVerifier} 会抛
 * {@link IllegalStateException}）、以及 {@code ddl-auto: validate} 与 Flyway 迁移脚本对不上（改实体忘写迁移）。
 * 两者都不会被编译、单元测试或 SpotBugs 拦住，只会让服务在部署时起不来。
 */
@SpringBootTest
@ActiveProfiles("test") // 关掉 Nacos 注册与配置导入；Flyway 与 validate 刻意保持开启，见 application-test.yml
@Tag("integration") // 依赖本机 MySQL/Redis/RabbitMQ：默认从 test/check 排除，容器就绪时用 -PintegrationTests 跑
class EcsServiceApplicationTests {

    @Test
    void contextLoads() {}
}
