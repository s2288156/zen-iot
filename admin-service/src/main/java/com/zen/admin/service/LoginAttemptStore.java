package com.zen.admin.service;

/**
 * 登录失败计数与锁定状态存储（G4-1：接口与实现都放在 service 包）。
 *
 * <p>Redis 异常在实现内部收敛并 fail-open（G4-1），调用方不感知存储故障；键语义见 {@link RedisLoginAttemptStore}。
 */
public interface LoginAttemptStore {

    /** 账号当前是否处于锁定期。 */
    boolean isLocked(String username);

    /**
     * 记一次凭据失败。
     *
     * @return 本次失败是否恰好触发锁定
     */
    boolean recordFailure(String username);

    /** 登录成功后清掉失败计数；锁定期内不会走到成功路径，锁键无需处理。 */
    void reset(String username);
}
