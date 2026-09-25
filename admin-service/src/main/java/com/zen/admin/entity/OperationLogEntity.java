package com.zen.admin.entity;

import com.zen.common.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 操作审计日志。与 {@link LoginLogEntity} 同样只写不读（G5-1）：由 {@code OperationLogInterceptor}
 * 在 service 事务外落库，业务回滚不丢审计行；查询接口等有真实需求再开。
 *
 * <p>本表没有 {@code deleted} 列，因此刻意不加 {@code @SQLRestriction}——审计流水不做逻辑删除。
 */
@Getter
@Setter
@Entity
@Table(name = "t_operation_log")
public class OperationLogEntity extends BaseEntity {

    /** 操作人用户名，取自 {@code UserContext}，与 t_login_log.username 同宽。 */
    @Column(name = "operator", nullable = false, length = 64)
    private String operator;

    /** 模块码，由 handler 方法上的 {@code @RequireModule} 推导，与模块权限体系同源。 */
    @Column(name = "module", nullable = false, length = 32)
    private String module;

    /** 动作，取值见 {@code OperationAction} 词表（对外契约，V5 建表注释同步）。 */
    @Column(name = "action", nullable = false, length = 32)
    private String action;

    /** 目标类型，取值见 {@code OperationTargetType} 词表。 */
    @Column(name = "target_type", nullable = false, length = 32)
    private String targetType;

    /** 目标 id 的路径变量文本形态；创建类操作留空，落库即定不回填（G5-1）。 */
    @Column(name = "target_id", length = 64)
    private String targetId;

    /** 响应的 HTTP 状态码；INT 对应 Integer，primitive int 会让 ddl-auto=validate 报列可空性不符。 */
    @Column(name = "result_code", nullable = false)
    private Integer resultCode;

    @Column(name = "op_time", nullable = false)
    private LocalDateTime opTime;
}
