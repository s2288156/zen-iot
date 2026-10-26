-- 种子数据：admin 角色（全部模块）+ user 角色（业务模块），对应 admin / demo 两个账号
-- 缺 demo 账号就无法验证「非管理员被 403」这条验收标准，两者缺一不可
-- 口令密文经 Flyway 占位符注入，来源 spring.flyway.placeholders.*；生产以环境变量覆盖
--   SPRING_FLYWAY_PLACEHOLDERS_ADMINPASSWORD / SPRING_FLYWAY_PLACEHOLDERS_DEMOPASSWORD
-- creator 显式写 system：迁移脚本不经过 JPA 审计

INSERT INTO t_role (role_code, role_name, description, creator)
VALUES ('admin', '管理员', '拥有全部模块访问权限', 'system'),
       ('user', '普通用户', '仅拥有业务模块访问权限', 'system');

INSERT INTO t_user (username, password, status, creator)
VALUES ('admin', '${adminPassword}', 1, 'system'),
       ('demo', '${demoPassword}', 1, 'system');

INSERT INTO t_user_role (user_id, role_id)
VALUES ((SELECT id FROM t_user WHERE username = 'admin'),
        (SELECT id FROM t_role WHERE role_code = 'admin')),
       ((SELECT id FROM t_user WHERE username = 'demo'),
        (SELECT id FROM t_role WHERE role_code = 'user'));

INSERT INTO t_role_module (role_id, module_code)
VALUES ((SELECT id FROM t_role WHERE role_code = 'admin'), 'admin'),
       ((SELECT id FROM t_role WHERE role_code = 'admin'), 'ecs'),
       ((SELECT id FROM t_role WHERE role_code = 'admin'), 'wcs'),
       ((SELECT id FROM t_role WHERE role_code = 'admin'), 'rcs'),
       ((SELECT id FROM t_role WHERE role_code = 'user'), 'ecs'),
       ((SELECT id FROM t_role WHERE role_code = 'user'), 'wcs'),
       ((SELECT id FROM t_role WHERE role_code = 'user'), 'rcs');
