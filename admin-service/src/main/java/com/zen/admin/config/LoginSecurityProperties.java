package com.zen.admin.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 登录失败锁定配置，前缀 {@code zen.security.login}（与 common-security 的 {@code zen.security} 同族不同节点）。 */
@Getter
@Setter
@ConfigurationProperties(prefix = "zen.security.login")
public class LoginSecurityProperties {

    /** 计数窗口内允许的最大失败次数；第 N 次失败即触发锁定。 */
    private int maxFailAttempts = 5;

    /** 锁定时长，也是 {@code auth:lock} 键的 TTL。 */
    private Duration lockoutTtl = Duration.ofMinutes(15);

    /** 失败计数窗口，即 {@code auth:fail} 键的 TTL（G1-A：只给首次 INCR 设置，不随后续失败续期）。 */
    private Duration failureWindowTtl = Duration.ofMinutes(15);
}
