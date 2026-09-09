package com.zen.common.core.security;

import java.time.Duration;

/**
 * Token 撤销检查。{@code common-core} 不依赖 Redis,实现由持有 Redis 的服务(admin-service、Phase 2 网关)提供。
 *
 * <p>没有实现时自动配置退化为不校验的空实现,此时登出只对本服务的 Token 无效。
 */
public interface TokenRevocationChecker {

    /** 按 Token 剩余有效期写入撤销记录,记录随 TTL 自动过期,不做清理任务。 */
    void revoke(String jti, Duration ttl);

    boolean isRevoked(String jti);

    /** 空实现:撤销无处可写,校验恒通过。 */
    static TokenRevocationChecker disabled() {
        return new TokenRevocationChecker() {
            @Override
            public void revoke(String jti, Duration ttl) {}

            @Override
            public boolean isRevoked(String jti) {
                return false;
            }
        };
    }
}
