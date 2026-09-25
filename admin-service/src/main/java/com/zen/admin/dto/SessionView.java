package com.zen.admin.dto;

import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * 在线会话视图。刻意不含 {@code currentRefreshJti}/{@code lastAccessJti}：jti 是撤销粒度的内部标识，
 * 泄露给前端既无用途也给重放排查添乱；管理接口只需要「谁、从哪、什么时候、还能在线多久」。
 */
public record SessionView(
        String sessionId,
        long userId,
        String username,
        @Nullable String ip,
        @Nullable String userAgent,
        Instant issueTime,
        Instant expireTime) {}
