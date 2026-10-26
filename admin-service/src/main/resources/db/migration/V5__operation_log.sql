-- Phase 5 操作审计：操作日志表
-- 全库统一 utf8mb4 / utf8mb4_0900_ai_ci（MySQL 8 默认，UCA 9.0）：建表语句不得省略 COLLATE，
-- 否则未显式声明的表会落到库级默认值上，与其余表混用排序规则
-- 审计列名与 common-core 的 BaseEntity 一致：creator / create_time / updater / update_time
-- 本表没有 deleted 列：日志是追加写的审计流水，不做逻辑删除，实体侧自然也没有 @SQLRestriction
-- op_time 是业务列而非复用 create_time：操作完成时刻由 OperationLogInterceptor 在事务外显式写入；
-- 审计行 best-effort 落库，业务回滚不丢行，排查时以 op_time 为准
-- target_id 创建类留空：新资源主键在业务事务内生成，拦截器拿不到，「落库即定不回填」（G5-1）；
-- 因此它是字符串列——同列还可能存 sessionId 等 UUID，统一按路径变量的文本形态存
-- result_code 存响应的 HTTP 状态码（决策：不是业务错误码）；ex 非空路径同样从 response.getStatus() 取值

CREATE TABLE t_operation_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    operator    VARCHAR(64)  NOT NULL COMMENT '操作人用户名',
    module      VARCHAR(32)  NOT NULL COMMENT '模块码: admin/wcs/rcs/ecs，由 @RequireModule 推导',
    action      VARCHAR(32)  NOT NULL COMMENT '动作: CREATE/UPDATE/DELETE/ASSIGN_MODULES/ASSIGN_ROLES/CHANGE_STATUS/RESET_PASSWORD',
    target_type VARCHAR(32)  NOT NULL COMMENT '目标类型: USER/ROLE/SESSION',
    target_id   VARCHAR(64)  DEFAULT NULL COMMENT '目标 id（路径变量），创建类为空',
    result_code INT          NOT NULL COMMENT '响应的 HTTP 状态码',
    op_time     DATETIME     NOT NULL COMMENT '操作完成时间',
    creator     VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater     VARCHAR(64)  DEFAULT NULL COMMENT '更新人',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_operator_op_time (operator, op_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '操作审计日志表';
