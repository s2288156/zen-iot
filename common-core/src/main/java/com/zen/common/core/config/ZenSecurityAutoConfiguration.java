package com.zen.common.core.config;

import com.zen.common.core.jwt.JwtTokenVerifier;
import com.zen.common.core.security.AuthInterceptor;
import com.zen.common.core.security.GatewayHeaderUserContextResolver;
import com.zen.common.core.security.JwtUserContextResolver;
import com.zen.common.core.security.ModuleAuthInterceptor;
import com.zen.common.core.security.SecurityProperties;
import com.zen.common.core.security.TokenRevocationChecker;
import com.zen.common.core.security.UserContextResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 认证拦截的自动配置:注册 {@link AuthInterceptor} 与身份来源,并把 {@code zen.security.whitelist} 交给 {@code
 * InterceptorRegistry#excludePathPatterns} 匹配。
 *
 * <p>必须限定 Servlet Web 应用:Phase 2 的 WebFlux 网关同样依赖 {@code common-core},Servlet 拦截器与 {@link
 * WebMvcConfigurer} 漏到网关类路径上就是启动失败。
 *
 * <p>{@code zen.security.enabled=false} 会整体跳过鉴权,只供本地开发与测试,生产环境不得关闭。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "zen.security", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(SecurityProperties.class)
public class ZenSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TokenRevocationChecker.class)
    public TokenRevocationChecker disabledTokenRevocationChecker() {
        return TokenRevocationChecker.disabled();
    }

    @Bean
    @ConditionalOnMissingBean(UserContextResolver.class)
    @ConditionalOnProperty(prefix = "zen.security", name = "context-source", havingValue = "jwt", matchIfMissing = true)
    public UserContextResolver jwtUserContextResolver(
            ObjectProvider<JwtTokenVerifier> verifier, TokenRevocationChecker revocationChecker) {
        return new JwtUserContextResolver(
                verifier.getIfAvailable(() -> {
                    throw new IllegalStateException("zen.security.context-source=jwt 需要 zen.jwt.secret（UTF-8 ≥ 32 字节）；"
                            + "本服务不解析 Token 时请改为 context-source=gateway-header");
                }),
                revocationChecker);
    }

    @Bean
    @ConditionalOnMissingBean(UserContextResolver.class)
    @ConditionalOnProperty(prefix = "zen.security", name = "context-source", havingValue = "gateway-header")
    public UserContextResolver gatewayHeaderUserContextResolver() {
        return new GatewayHeaderUserContextResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthInterceptor authInterceptor(UserContextResolver resolver) {
        return new AuthInterceptor(resolver);
    }

    @Bean
    @ConditionalOnMissingBean
    public ModuleAuthInterceptor moduleAuthInterceptor() {
        return new ModuleAuthInterceptor();
    }

    @Bean
    public WebMvcConfigurer zenSecurityWebMvcConfigurer(
            AuthInterceptor authInterceptor,
            ModuleAuthInterceptor moduleAuthInterceptor,
            SecurityProperties properties) {
        String[] whitelist = properties.getWhitelist().toArray(String[]::new);
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                // 注册顺序即执行顺序：模块校验要读鉴权写入的 UserContext，不能排在前面
                registry.addInterceptor(authInterceptor).addPathPatterns("/**").excludePathPatterns(whitelist);
                registry.addInterceptor(moduleAuthInterceptor)
                        .addPathPatterns("/**")
                        .excludePathPatterns(whitelist);
            }
        };
    }
}
