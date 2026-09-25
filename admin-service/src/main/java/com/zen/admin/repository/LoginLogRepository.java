package com.zen.admin.repository;

import com.zen.admin.entity.LoginLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 登录日志仓储。本期只暴露 {@code JpaRepository} 自带的 {@code save}：G3-A 定了只写不读，
 * 任何查询方法等出现真实消费方再加，避免无人调用的死接口。
 */
public interface LoginLogRepository extends JpaRepository<LoginLogEntity, Long> {}
