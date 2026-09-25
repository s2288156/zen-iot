package com.zen.admin.service;

import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * 在线会话的登记形态，即 {@code auth:session:{sessionId}} 键里的 JSON 形状（G5-2 钉死八字段，不多不少）。
 *
 * <p>会话 = refresh 令牌谱系：{@code currentRefreshJti} 是当前有效的 refresh jti（refresh 轮转会换），
 * {@code lastAccessJti} 是最近一次签发 access 的 jti。{@code expireTime} 跟随 refresh 有效期，
 * 轮转后与登记键 TTL、ZSET score 三者一并迁移（决策 (b)）；{@code issueTime} 保持首次登录时刻不变。
 *
 * <p>{@code userId}/{@code username} 冗余存储：ZSET 索引只按过期序，列会话与按人吊销都不该为定位身份
 * 再回查数据库。
 */
public record SessionInfo(
        String sessionId,
        long userId,
        String username,
        @Nullable String ip,
        @Nullable String userAgent,
        Instant issueTime,
        Instant expireTime,
        String currentRefreshJti,
        String lastAccessJti) {}
