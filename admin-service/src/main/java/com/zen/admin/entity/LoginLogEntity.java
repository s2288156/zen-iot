package com.zen.admin.entity;

import com.zen.common.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 登录尝试日志。本期只写不读（G3-A）：为锁定与登录失败排查留痕，查询接口等有真实需求再开。
 *
 * <p>本表没有 {@code deleted} 列，因此刻意不加 {@code @SQLRestriction}——审计流水不做逻辑删除。
 */
@Getter
@Setter
@Entity
@Table(name = "t_login_log")
public class LoginLogEntity extends BaseEntity {

    /** 登录成功，对应 t_login_log.success 的 TINYINT；改成 Integer 会让 ddl-auto=validate 报列类型不符。 */
    public static final byte RESULT_SUCCESS = 1;

    /** 登录失败，对应 t_login_log.success 的 TINYINT。 */
    public static final byte RESULT_FAILURE = 0;

    @Column(name = "username", nullable = false, length = 64)
    private String username;

    @Column(name = "success", nullable = false)
    private Byte success;

    /** 结果原因，取值见 AuthService 的 reason 词表：SUCCESS / BAD_CREDENTIALS / LOCKED / DISABLED。 */
    @Column(name = "reason", nullable = false, length = 32)
    private String reason;

    @Column(name = "ip", length = 64)
    private String ip;

    @Column(name = "user_agent", length = 256)
    private String userAgent;

    @Column(name = "login_time", nullable = false)
    private LocalDateTime loginTime;
}
