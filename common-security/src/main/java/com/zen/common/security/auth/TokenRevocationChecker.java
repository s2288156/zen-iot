package com.zen.common.security.auth;

import java.time.Duration;

/**
 * Token 撤销检查。{@code common-security} 不依赖 Redis,实现由持有 Redis 的服务(admin-service、Phase 2 网关)提供。
 *
 * <p>没有实现时自动配置退化为不校验的空实现,此时登出只对本服务的 Token 无效。
 */
public interface TokenRevocationChecker {

    /**
     * 黑名单 Key 前缀,后面接 Token 的 {@code jti}。写入方(admin-service)与读取方(Phase 2 网关)必须共用这一份字面量:
     * 两边各写一遍时,改错了不会编译失败也不会报错,只会让登出的 Token 在网关侧继续可用——属于安全失效,故收敛到单一来源。
     */
    String BLACKLIST_KEY_PREFIX = "auth:blacklist:";

    /** 拼出某个 jti 的黑名单 Key。 */
    static String blacklistKey(String jti) {
        return BLACKLIST_KEY_PREFIX + jti;
    }

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
