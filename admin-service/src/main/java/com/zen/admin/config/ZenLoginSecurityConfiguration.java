package com.zen.admin.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** 只负责注册 {@link LoginSecurityProperties}；Store 与 Service Bean 走各自的组件扫描。 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(LoginSecurityProperties.class)
public class ZenLoginSecurityConfiguration {}
