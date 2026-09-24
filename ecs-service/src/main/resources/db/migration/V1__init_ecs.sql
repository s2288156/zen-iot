-- Phase 3 ECS 设备管理与协议适配框架：设备档案、分组、上下线事件、指令中转
-- 全库统一 utf8mb4 / utf8mb4_0900_ai_ci（与 admin-service 及 MySQL 8 服务器默认一致），建表不得省略 COLLATE
-- 审计列名与 common-core 的 BaseEntity 一致：creator / create_time / updater / update_time
-- deleted 不在实体上映射：DDL 默认 0，t_device / t_device_group 只通过 @SQLRestriction("deleted = 0") 过滤，
-- 写入侧由同一批实体上的 @SQLDelete 把 delete 语句换成 UPDATE deleted = 1
-- 在线状态只由心跳维护，判定读的是应用侧注入的 Clock，occurred_at / last_heartbeat_time 都写入应用算出的时刻

CREATE TABLE t_device_group (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    group_code  VARCHAR(64)  NOT NULL COMMENT '分组编码',
    group_name  VARCHAR(64)  NOT NULL COMMENT '分组名称',
    description VARCHAR(255) DEFAULT NULL COMMENT '描述',
    creator     VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater     VARCHAR(64)  DEFAULT NULL COMMENT '更新人',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0否 1是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_group_code (group_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备分组表';

CREATE TABLE t_device (
    id                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    device_code         VARCHAR(64)  NOT NULL COMMENT '设备编码，全局唯一，MQ 指令按它定位设备',
    device_name         VARCHAR(64)  NOT NULL COMMENT '设备名称',
    group_id            BIGINT       DEFAULT NULL COMMENT '所属分组，可空表示未分组',
    protocol_type       VARCHAR(32)  NOT NULL COMMENT '协议类型：Phase 3 只有 loopback，Phase 4 追加 modbus-tcp / opc-ua',
    endpoint            VARCHAR(255) NOT NULL COMMENT '设备地址，由对应协议适配器解析（如 tcp://127.0.0.1:502/1）',
    online              TINYINT      NOT NULL DEFAULT 0 COMMENT '在线状态：1在线 0离线',
    last_heartbeat_time DATETIME     DEFAULT NULL COMMENT '最近一次心跳时刻，仅用于展示；超时判定读 Redis 与注入的 Clock',
    creator             VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
    create_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater             VARCHAR(64)  DEFAULT NULL COMMENT '更新人',
    update_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted             TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0否 1是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_device_code (device_code),
    KEY idx_group_id (group_id),
    KEY idx_online (online)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备档案表';

-- 上下线事件独立成表：t_device.online 只保留当前态，历史（谁在何时上线/离线、因何离线）靠它回答
CREATE TABLE t_device_event (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    device_id   BIGINT      NOT NULL COMMENT '设备ID',
    event_type  VARCHAR(16) NOT NULL COMMENT '事件类型：ONLINE / OFFLINE',
    occurred_at DATETIME    NOT NULL COMMENT '事件发生时刻（取注入的 Clock）',
    reason      VARCHAR(255) DEFAULT NULL COMMENT '触发原因，如 心跳上报 / 心跳超时',
    creator     VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater     VARCHAR(64)  DEFAULT NULL COMMENT '更新人',
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_device_id_occurred_at (device_id, occurred_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备上下线事件表';

-- 指令中转的落库面：command_no 是幂等键，重复投递不会执行第二次
CREATE TABLE t_device_command (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    command_no    VARCHAR(64)  NOT NULL COMMENT '指令号，来自 RCS，唯一约束承担幂等',
    device_id     BIGINT       DEFAULT NULL COMMENT '目标设备ID；编码查无此设备时为空，只留痕不执行',
    command_type  VARCHAR(32)  NOT NULL COMMENT '指令类型，如 MOVE / PICK / PUT',
    payload       VARCHAR(1024) DEFAULT NULL COMMENT '指令负载，原样透传给协议适配器',
    status        VARCHAR(16)  NOT NULL COMMENT '执行状态：PENDING / SUCCESS / FAILED',
    retry_count   INT          NOT NULL DEFAULT 0 COMMENT '已重试次数（不含首次执行）',
    error_message VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
    finish_time   DATETIME     DEFAULT NULL COMMENT '终态时刻（取注入的 Clock）',
    creator       VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater       VARCHAR(64)  DEFAULT NULL COMMENT '更新人',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_command_no (command_no),
    KEY idx_device_id (device_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备指令中转表';
