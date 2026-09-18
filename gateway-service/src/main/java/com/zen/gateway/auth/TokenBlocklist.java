package com.zen.gateway.auth;

import reactor.core.publisher.Mono;

/**
 * 网关侧的 Token 撤销读取：只查黑名单，撤销由 admin-service 写入（见 Phase 1 的 {@code RedisTokenRevocationChecker}）。
 *
 * <p>刻意不复用 {@code common-core} 的 {@code TokenRevocationChecker}：那个接口是命令式的，在 Netty EventLoop 上调用它就是
 * Phase 2 点名的第一个 BlockHound 类问题。实现必须返回**不会空完成**的 {@code Mono<Boolean>}（空完成会让请求既不放行也不拒绝，直接挂住），过滤器据此决定放行还是拒绝。
 */
public interface TokenBlocklist {

    /**
     * @param jti Token 唯一 ID
     * @return 已撤销为 {@code true}；后端不可用时应以错误信号透出，由网关兜成 5xx（宁可拒绝也不放行）
     */
    Mono<Boolean> isBlocked(String jti);
}
