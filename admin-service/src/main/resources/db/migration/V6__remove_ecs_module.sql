-- ecs-service 已移除，ModuleCode 不再有 ecs；清理 t_role_module 的残留授权行。
-- 只做 DELETE，不动其他模块的行：空库回放时 V2 仍会种下 ecs 行，这里一并清掉，保证新旧库终态一致。
DELETE FROM t_role_module WHERE module_code = 'ecs';
