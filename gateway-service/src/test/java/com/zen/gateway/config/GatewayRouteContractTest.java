package com.zen.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.zen.common.security.jwt.JwtTokenVerifier;
import java.util.List;
import java.util.Objects;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.context.ApplicationContext;

/**
 * 路由与配置的契约测试：直接断言 {@code application.yml} 绑定出来的结果，不启服务器、不碰中间件。
 *
 * <p>{@link com.zen.gateway.GatewayRoutingTest} 只在 {@code /api/admin} 这一条路由上跑真实请求，另外三条（wcs/rcs/ecs）
 * 写错了一律没人发现——路径段与服务名各写一遍，正是最容易写岔的地方。这里逐条比对四段前缀。
 *
 * <p>顺带把模块边界的启动期后果钉住：网关上下文里不许出现 {@code DataSource}，且 {@code common-core} 的 JWT
 * 自动装配要在排掉三个 starter 之后仍然生效。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {"spring.config.import=", "spring.cloud.nacos.discovery.enabled=false"})
class GatewayRouteContractTest {

    @Autowired
    private GatewayProperties gatewayProperties;

    @Autowired
    private ZenGatewayProperties zenGatewayProperties;

    @Autowired
    private ApplicationContext context;

    @Test
    void everyBusinessModuleIsRoutedWithPrefixStripped() {
        List<RouteDefinition> routes = gatewayProperties.getRoutes();
        assertThat(routes)
                .extracting(RouteDefinition::getId)
                .containsExactly("admin-service", "wcs-service", "rcs-service", "ecs-service");

        for (RouteDefinition route : routes) {
            // RouteDefinition.getId() 在规范化前可为 null（SCG 的签名），这里显式要求非空而不是悄悄跳过
            String id = Objects.requireNonNull(route.getId(), "路由缺少 id");
            String service = id.replace("-service", "");
            // 前缀归网关：/api/{service} 两段由 StripPrefix 剥掉，下游服务内不含 /api
            assertThat(route.getUri().toString()).isEqualTo("lb://" + id);
            assertThat(route.getPredicates()).hasSize(1);
            assertThat(route.getPredicates().getFirst().getName()).isEqualTo("Path");
            assertThat(route.getPredicates().getFirst().getArgs().values()).containsExactly("/api/" + service + "/**");
            assertThat(route.getFilters()).hasSize(1);
            assertThat(route.getFilters().getFirst().getName()).isEqualTo("StripPrefix");
            assertThat(route.getFilters().getFirst().getArgs().values()).containsExactly("2");
        }
    }

    /** 白名单匹配的是带前缀的入站路径，照抄服务内的裸路径会全部漏放。 */
    @Test
    void whitelistUsesGatewayPrefixedInboundPaths() {
        assertThat(zenGatewayProperties.getAuth().getWhitelist())
                .contains("/api/admin/auth/login", "/api/admin/auth/refresh", "/api/*/actuator/health");
    }

    @Test
    void jwtVerifierIsAutoConfiguredDespiteCommonCoreExcludes() {
        assertThat(context.getBean(JwtTokenVerifier.class)).isNotNull();
    }

    @Test
    void noDataSourceIsWiredIntoTheGateway() {
        // data-jpa 没排干净时的表现是 DataSourceAutoConfiguration 直接抛；这里断言连 Bean 都没有
        assertThat(context.getBeanProvider(DataSource.class).getIfAvailable()).isNull();
    }
}
