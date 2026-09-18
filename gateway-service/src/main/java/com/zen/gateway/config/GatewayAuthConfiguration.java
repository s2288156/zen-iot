package com.zen.gateway.config;

import com.zen.common.security.jwt.JwtTokenVerifier;
import com.zen.gateway.auth.RedisTokenBlocklist;
import com.zen.gateway.auth.TokenBlocklist;
import com.zen.gateway.filter.JwtAuthGlobalFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import tools.jackson.databind.ObjectMapper;

/**
 * 鉴权链路的 Bean 装配：反应式黑名单 + 全局鉴权过滤器。
 *
 * <p>{@code JwtTokenVerifier} 由 {@code common-security} 的 {@code ZenJwtAutoConfiguration} 自动装配（以
 * {@code zen.jwt.secret} 存在为开关），本类不重复声明。
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ZenGatewayProperties.class)
public class GatewayAuthConfiguration {

    @Bean
    public TokenBlocklist tokenBlocklist(ReactiveStringRedisTemplate redisTemplate) {
        return new RedisTokenBlocklist(redisTemplate);
    }

    // 刻意不提供「关闭网关鉴权」的开关：网关是唯一的身份来源，能被配置关掉就等于没有鉴权
    @Bean
    public JwtAuthGlobalFilter jwtAuthGlobalFilter(
            JwtTokenVerifier tokenVerifier,
            TokenBlocklist tokenBlocklist,
            ObjectMapper objectMapper,
            ZenGatewayProperties properties) {
        return new JwtAuthGlobalFilter(
                tokenVerifier,
                tokenBlocklist,
                objectMapper,
                properties.getAuth().getWhitelist());
    }
}
