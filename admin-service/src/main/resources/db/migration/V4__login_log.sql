-- Phase 4 登录安全：登录尝试日志表
-- 全库统一 utf8mb4 / utf8mb4_0900_ai_ci（MySQL 8 默认，UCA 9.0）：建表语句不得省略 COLLATE，
-- 否则未显式声明的表会落到库级默认值上，与其余表混用排序规则
-- 审计列名与 common-core 的 BaseEntity 一致：creator / create_time / updater / update_time
-- 本表没有 deleted 列：日志是追加写的审计流水，不做逻辑删除，实体侧自然也没有 @SQLRestriction
-- login_time 是业务列而非复用 create_time：登录尝试发生的时刻由服务端显式写入，
-- 与 JPA Auditing 的入库时间解耦（时钟来源不同，排查时以 login_time 为准）

CREATE TABLE t_login_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(64)  NOT NULL COMMENT '登录用户名',
    success     TINYINT      NOT NULL DEFAULT 0 COMMENT '结果: 1成功 0失败',
    reason      VARCHAR(32)  NOT NULL COMMENT '原因: SUCCESS/BAD_CREDENTIALS/LOCKED/DISABLED',
    ip          VARCHAR(64)  DEFAULT NULL COMMENT '客户端IP',
    user_agent  VARCHAR(256) DEFAULT NULL COMMENT 'User-Agent',
    login_time  DATETIME     NOT NULL COMMENT '登录尝试时间',
    creator     VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater     VARCHAR(64)  DEFAULT NULL COMMENT '更新人',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_username_login_time (username, login_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '登录日志表';
