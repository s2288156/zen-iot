package com.zen.gateway.config;

import com.zen.common.security.auth.SecurityProperties;
import java.time.Duration;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 网关自有配置，前缀 {@code zen.gateway}。
 *
 * <p>刻意不复用 {@code common-security} 的 {@link SecurityProperties}（前缀 {@code zen.security}）：那份配置由带
 * {@code @ConditionalOnWebApplication(SERVLET)} 的自动配置绑定，反应式网关里根本不装配；而且两边的白名单匹配的是不同字符串——
 * 网关匹配带前缀的入站路径，服务匹配剥掉前缀后的裸路径。
 */
@Getter
@ConfigurationProperties(prefix = "zen.gateway")
public class ZenGatewayProperties {

    private final Auth auth = new Auth();

    private final Cors cors = new Cors();

    /** 鉴权相关配置。 */
    @Getter
    @Setter
    public static class Auth {

        /** 免鉴权路径（{@code PathPattern} 风格，如 {@code /api/admin/auth/refresh}），匹配的是带前缀的入站路径。 */
        private List<String> whitelist = List.of();
    }

    /** 跨域配置，由 {@code GatewayCorsConfiguration} 交给 {@code CorsWebFilter} 消费。 */
    @Getter
    @Setter
    public static class Cors {

        /** 允许的来源模式；用 patterns 而非 origins，才能同时开 {@code allowCredentials}。 */
        private List<String> allowedOriginPatterns = List.of("*");

        private List<String> allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

        private List<String> allowedHeaders = List.of("*");

        /** 默认关：前端凭 Bearer Token 认证，不需要带 Cookie；开它就得同时收窄 {@code allowedOriginPatterns}。 */
        private boolean allowCredentials;

        /** 预检结果缓存时长。 */
        private Duration maxAge = Duration.ofHours(1);
    }
}
