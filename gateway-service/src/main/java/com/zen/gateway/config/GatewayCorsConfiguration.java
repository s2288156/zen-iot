package com.zen.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * 统一跨域放行。
 *
 * <p>用 `CorsWebFilter` 而不是网关的 {@code globalcors} 配置：前者是 {@code WebFilter}，预检请求（不带
 * {@code Authorization}）在这里就被短路，不会进到鉴权过滤器；两者同时配还会重复写 {@code Access-Control-*} 头，浏览器反而拒绝。
 *
 * <p>顺序取 {@link Ordered#HIGHEST_PRECEDENCE}，由 {@link GatewayCorsWebFilter} 显式带上：预检必须早于鉴权与路由转发。
 */
@Configuration(proxyBeanMethods = false)
public class GatewayCorsConfiguration {

    @Bean
    public GatewayCorsWebFilter gatewayCorsWebFilter(ZenGatewayProperties properties) {
        ZenGatewayProperties.Cors cors = properties.getCors();
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(cors.getAllowedOriginPatterns());
        configuration.setAllowedMethods(cors.getAllowedMethods());
        configuration.setAllowedHeaders(cors.getAllowedHeaders());
        configuration.setAllowCredentials(cors.isAllowCredentials());
        configuration.setMaxAge(cors.getMaxAge());
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return new GatewayCorsWebFilter(source, Ordered.HIGHEST_PRECEDENCE);
    }
}
