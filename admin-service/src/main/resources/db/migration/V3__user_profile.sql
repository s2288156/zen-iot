-- Phase 2 用户管理：t_user 扩展资料列，全部可空（存量种子用户不回填）
-- 列级显式声明 COLLATE，与 V1 建表注释的"全库 utf8mb4_0900_ai_ci"约定保持一致
-- email/phone 不加唯一约束：业务未要求以邮箱/手机号定位账号，唯一性留给后续按需收紧
ALTER TABLE t_user
    ADD COLUMN nickname VARCHAR(64)  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '昵称',
    ADD COLUMN email    VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '邮箱',
    ADD COLUMN phone    VARCHAR(32)  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '手机号',
    ADD COLUMN avatar   VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '头像URL';
