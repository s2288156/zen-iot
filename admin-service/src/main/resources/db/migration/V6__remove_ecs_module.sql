-- 删除 ecs 模块后，清理已有 DB 里 t_role_module 的残留授权行
-- 迁移脚本只做 DELETE，不改动其他模块的行
DELETE FROM t_role_module WHERE module_code = 'ecs';
