package com.zen.ecs.config;

import com.zen.common.security.auth.UserContext;
import com.zen.common.security.auth.UserPrincipal;
import com.zen.ecs.protocol.DeviceAdapterFactory;
import com.zen.ecs.protocol.ProtocolSupport;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.scheduling.annotation.EnableScheduling;

/** ECS 的基础 Bean。 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(ZenEcsProperties.class)
public class ZenEcsConfiguration {

    /** 时间源。心跳超时判定与事件时刻一律读它，测试换成固定/拨快的时钟就能直接断言「超时自动离线」，不必真等阈值。 */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    /**
     * 协议适配器工厂。入参由 Spring 收集全部 {@link ProtocolSupport} Bean（当前只有 loopback，Phase 4 追加 Modbus / OPC UA）。
     *
     * <p>做成 Bean 而不是 {@code @Component}：工厂本身与协议无关，让它由配置装配，协议实现才只依赖接口。
     */
    @Bean
    public DeviceAdapterFactory deviceAdapterFactory(List<ProtocolSupport> supports) {
        return new DeviceAdapterFactory(supports);
    }

    /** 覆盖 common-core 的默认审计人 {@code system}；自动配置在用户 Bean 之后评估，会自动退让（与 admin-service 同口径）。 */
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.ofNullable(UserContext.get()).map(UserPrincipal::username);
    }
}
