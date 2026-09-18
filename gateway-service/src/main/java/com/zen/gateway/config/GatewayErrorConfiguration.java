package com.zen.gateway.config;

import com.zen.gateway.error.GatewayErrorWebExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

/** 网关级错误的统一 JSON 出口（鉴权失败由过滤器自己写出，不经此处）。 */
@Configuration(proxyBeanMethods = false)
public class GatewayErrorConfiguration {

    @Bean
    public GatewayErrorWebExceptionHandler gatewayErrorWebExceptionHandler(ObjectMapper objectMapper) {
        return new GatewayErrorWebExceptionHandler(objectMapper);
    }
}
