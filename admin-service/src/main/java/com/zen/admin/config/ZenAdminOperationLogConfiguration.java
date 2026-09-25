package com.zen.admin.config;

import com.zen.admin.interceptor.OperationLogInterceptor;
import com.zen.admin.repository.OperationLogRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 操作审计拦截器的装配。order 必须显式大于安全拦截器（{@code ZenSecurityAutoConfiguration} 以默认 0 注册），
 * 这样 {@code afterCompletion} 按注册逆序回调时审计最先跑、{@code UserContext} 尚未清除。
 *
 * <p>不排除白名单路径：拦截器以「handler 方法是否带 {@code @OperationLog}」为准，无注解即零开销直通。
 */
@Configuration(proxyBeanMethods = false)
public class ZenAdminOperationLogConfiguration {

    @Bean
    public OperationLogInterceptor operationLogInterceptor(OperationLogRepository operationLogRepository) {
        return new OperationLogInterceptor(operationLogRepository);
    }

    @Bean
    public WebMvcConfigurer operationLogWebMvcConfigurer(OperationLogInterceptor interceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(interceptor).addPathPatterns("/**").order(100);
            }
        };
    }
}
