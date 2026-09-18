package com.zen.gateway.config;

import org.springframework.core.Ordered;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsWebFilter;

/**
 * 带执行顺序的 {@link CorsWebFilter}。
 *
 * <p>必须存在：{@code WebHttpHandlerBuilder} 收集 {@code WebFilter} Bean 后用 {@code AnnotationAwareOrderComparator} 排序，
 * 而 {@code CorsWebFilter} 既没实现 {@code Ordered}、也没有类级 {@code @Order}——只把 {@code @Order} 写在 {@code @Bean}
 * 方法上对这条排序路径无效，它会落到 {@code LOWEST_PRECEDENCE}，排在网关路由转发之后。预检请求（不带
 * {@code Authorization}）于是被鉴权过滤器判成 401，浏览器只看到「CORS 失败」。
 */
public class GatewayCorsWebFilter extends CorsWebFilter implements Ordered {

    private final int order;

    public GatewayCorsWebFilter(CorsConfigurationSource source, int order) {
        super(source);
        this.order = order;
    }

    @Override
    public int getOrder() {
        return order;
    }
}
