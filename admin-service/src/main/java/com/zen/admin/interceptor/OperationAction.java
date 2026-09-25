package com.zen.admin.interceptor;

/**
 * 操作动作词表（决策 (a)）：枚举名即 {@code t_operation_log.action} 的落库值，覆盖全部受审计写接口。
 *
 * <p>语义是操作动词本身，与 HTTP 方法/路径解耦——同为 PUT 的改资料与重置口令分别记
 * {@link #UPDATE} 与 {@link #RESET_PASSWORD}。扩充词表即改变对外契约，须同步 V5 建表注释。
 */
public enum OperationAction {
    CREATE,
    UPDATE,
    DELETE,
    ASSIGN_MODULES,
    ASSIGN_ROLES,
    CHANGE_STATUS,
    RESET_PASSWORD
}
