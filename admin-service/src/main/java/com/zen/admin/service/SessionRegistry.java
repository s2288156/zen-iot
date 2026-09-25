package com.zen.admin.service;

import java.util.List;
import java.util.Optional;

/**
 * 在线会话登记仓储（G5-2 键契约，Redis 实现见 {@link RedisSessionRegistry}）：
 *
 * <ul>
 *   <li>{@code auth:session:{sessionId}} — 会话登记 JSON，TTL = refresh 剩余有效期；
 *   <li>{@code auth:session:index} — ZSET 索引，member 为 sessionId，score 为过期毫秒，读时剔除死项；
 *   <li>{@code auth:session:refresh:{jti}} — 反向映射到 sessionId，供 refresh/logout 定位会话
 *       （JWT 载荷无会话 id，common-security 不改，映射建在 admin 侧）。
 * </ul>
 *
 * <p>本接口按「存储原语」划分，不吞任何 Redis 异常：fail-open/fail-closed 的分界由调用方
 * {@link SessionService} 按决策 (c) 决定——簿记路径 catch 降级，吊销路径上抛。
 */
public interface SessionRegistry {

    /** 写入/覆盖登记键、ZSET 索引项与当前 refresh 的反向映射；已过期（TTL ≤ 0）的谱系不登记。 */
    void save(SessionInfo session);

    Optional<SessionInfo> find(String sessionId);

    /** 按 refresh jti 定位 sessionId；映射缺失（旧谱系/已轮转）返回 empty。 */
    Optional<String> findSessionIdByRefreshJti(String refreshJti);

    /** 删除反向映射；轮转迁移完新登记后解除旧 jti 的映射。 */
    void unbindRefresh(String refreshJti);

    /** 删除登记键、ZSET 索引项与 {@code currentRefreshJti} 的反向映射。 */
    void remove(SessionInfo session);

    /** 在线会话总数；先剔除已过期索引项再计数。 */
    long countActive();

    /** 按过期时刻升序分页；先剔除死项，登记键竞态丢失的条目跳过（页内可能少于 limit，接受）。 */
    List<SessionInfo> listActive(long offset, long limit);

    /** 全量在线会话，供按用户吊销遍历；规模同为 admin 后台量级，不设分页。 */
    List<SessionInfo> listAllActive();
}
