package com.zen.ecs.presence;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * 设备在线状态的存储抽象：心跳只写这里，超时判定按注入的 {@code java.time.Clock} 算。
 *
 * <p>抽一层的唯一理由是「超时自动离线」必须能在没有 Redis 的机器上被测试——实现换成进程内的假存储，把时钟拨过阈值就能断言，
 * 不必真等 30 秒。分层门禁只看 {@code controller → service → repository → entity} 四层，本包在四层之外，
 * 因此实现方（service）依赖它不会被判跨层。
 */
public interface DevicePresenceStore {

    /** 记一次心跳。{@code ttl} 让键自行过期，避免设备被拆除后留下永久脏数据。 */
    void markAlive(long deviceId, Instant seenAt, Duration ttl);

    /** 最近一次心跳时刻；键已过期或从未上报时为 {@link Optional#empty()}。 */
    Optional<Instant> lastSeen(long deviceId);

    /** 判离线后清键：否则设备一直掉线，每个探测周期都会重复触发同一条离线事件。 */
    void clear(long deviceId);
}
