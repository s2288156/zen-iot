package com.zen.admin.interceptor;

/**
 * 操作目标类型词表（决策 (a)）：枚举名即 {@code t_operation_log.target_type} 的落库值。
 *
 * <p>{@link #SESSION} 为 Phase 5 强制下线（{@code DELETE /sessions/{sessionId}}）扩充；扩充须同步 V5 建表注释。
 */
public enum OperationTargetType {
    USER,
    ROLE,
    SESSION
}
