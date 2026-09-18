package com.zen.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;

/**
 * 类路径隔离门禁：把 {@code gateway-service/build.gradle.kts} 里对 {@code common-core} 的三个 exclude 变成可执行断言。
 *
 * <p>为什么要用 {@code Class.forName} 而不是只看编译期：这些坐标只要被某条新依赖重新带进来（换依赖、加 starter、Dependabot
 * 抬版本），编译照样过、IDE 也不报警，直到真启动才炸——「{@code starter-web} 上类路径 → Boot 判定为 Servlet 应用，WebFlux
 * 网关启动即失败」正是 Phase 2「关键决策」点名的那条。本测试让它在 {@code check} 里就变红。
 *
 * <p>只断言「 Servlet/MVC/JPA 这三类整包依赖」不存在；按类名拦不住的情况（如 {@code StringRedisTemplate} 与
 * {@code ReactiveStringRedisTemplate} 同在 {@code spring-data-redis} 一个 jar 里）交给
 * {@link ArchitectureTest} 按编译期依赖精确拦。
 */
class GatewayDependencyIsolationTest {

    /** 必须**不**在网关类路径上的类型，每项对应一种启动期或运行期失效模式。 */
    private static final List<String> MUST_BE_ABSENT = List.of(
            // spring-boot-starter-web 没排掉：应用被判成 Servlet 类型，WebFlux 网关启动即失败
            "jakarta.servlet.Servlet",
            "org.springframework.web.servlet.DispatcherServlet",
            "org.apache.catalina.startup.Tomcat",
            // spring-boot-starter-data-jpa 没排掉：无数据源时 DataSourceAutoConfiguration 直接炸
            "jakarta.persistence.Entity",
            "org.springframework.data.jpa.repository.support.SimpleJpaRepository",
            "org.hibernate.SessionFactory");

    /** 必须**在**网关类路径上的类型，缺了就是运行时才炸的隐形缺失。 */
    private static final List<String> MUST_BE_PRESENT = List.of(
            "org.springframework.cloud.gateway.config.GatewayProperties",
            "org.springframework.cloud.gateway.filter.GlobalFilter",
            "org.springframework.data.redis.core.ReactiveStringRedisTemplate",
            "com.alibaba.cloud.nacos.NacosDiscoveryProperties",
            "org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier",
            // jjwt 的实现与序列化器只出现在运行时（common-core 以 api 暴露 jjwt-api），漏了就等首次验签才炸
            "io.jsonwebtoken.impl.DefaultJwtBuilder",
            "io.jsonwebtoken.jackson.io.JacksonSerializer",
            // P3-1 的 A 层：网关要真的装配出 Tracer，traceId 才能跨网关串联
            "io.micrometer.tracing.Tracer");

    @Test
    void servletAndJpaStacksStayOffTheGatewayClasspath() {
        List<String> leaked = MUST_BE_ABSENT.stream().filter(this::isPresent).toList();
        assertThat(leaked)
                .as("网关类路径上出现了 Servlet 或 JPA 栈，build.gradle.kts 的 exclude 被绕过了")
                .isEmpty();
    }

    @Test
    void gatewayReactiveAndTracingStacksAreOnTheClasspath() {
        List<String> missing =
                MUST_BE_PRESENT.stream().filter(fqn -> !isPresent(fqn)).toList();
        assertThat(missing).as("网关运行期缺少这些类型，会在真启动或首次调用时才暴露").isEmpty();
    }

    /** Boot 的应用类型推断是本阶段最容易因一个依赖就整体翻车的开关（Boot 4 里它就叫 {@code deduce()}）。 */
    @Test
    void bootDeducesAReactiveApplication() {
        assertThat(WebApplicationType.deduce()).isEqualTo(WebApplicationType.REACTIVE);
    }

    private boolean isPresent(String className) {
        try {
            Class.forName(className, false, getClass().getClassLoader());
            return true;
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            return false;
        }
    }
}
