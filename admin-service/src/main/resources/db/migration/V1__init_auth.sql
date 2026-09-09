-- Phase 1 认证与模块级授权：用户 ↔ 角色 ↔ 模块 四表
-- 审计列名与 common-core 的 BaseEntity 一致：creator / create_time / updater / update_time
-- deleted 故意不在实体上映射：DDL 默认 0，实体只通过 @SQLRestriction("deleted = 0") 过滤

CREATE TABLE t_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(64)  NOT NULL COMMENT '用户名',
    password    VARCHAR(100) NOT NULL COMMENT '密码(BCrypt密文)',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 1启用 0禁用',
    creator     VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater     VARCHAR(64)  DEFAULT NULL COMMENT '更新人',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0否 1是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表';

CREATE TABLE t_role (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    role_code   VARCHAR(64)  NOT NULL COMMENT '角色编码',
    role_name   VARCHAR(64)  NOT NULL COMMENT '角色名称',
    description VARCHAR(255) DEFAULT NULL COMMENT '描述',
    creator     VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater     VARCHAR(64)  DEFAULT NULL COMMENT '更新人',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0否 1是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '角色表';

CREATE TABLE t_user_role (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (user_id, role_id),
    KEY idx_role_id (role_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户角色关联表';

CREATE TABLE t_role_module (
    role_id     BIGINT      NOT NULL COMMENT '角色ID',
    module_code VARCHAR(32) NOT NULL COMMENT '模块: admin/ecs/wcs/rcs',
    PRIMARY KEY (role_id, module_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '角色模块权限关联表';
