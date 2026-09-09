package com.zen.common.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * DAO 基础实体:主键 + 创建/更新审计字段。消费方实体 extends 本类即可。
 *
 * <p>审计字段由 JPA Auditing 维护,依赖 common-core 的 ZenJpaAuditingAutoConfiguration 生效。
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(name = "create_time", updatable = false, nullable = false)
    private LocalDateTime createTime;

    @LastModifiedDate
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @CreatedBy
    @Column(name = "creator", updatable = false, length = 64)
    private String creator;

    @LastModifiedBy
    @Column(name = "updater", length = 64)
    private String updater;
}
