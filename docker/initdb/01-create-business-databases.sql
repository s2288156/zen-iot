-- 业务库创建脚本：MySQL 镜像的 MYSQL_DATABASE 只能建一个库（这里是 zen_admin），其余业务库随各自阶段在此追加。
-- 执行时机：
--   本机 —— docker compose 首次创建 zen_mysql_data 卷时自动执行（数据卷已存在时需按 compose 顶部注释手工补一次）
--   CI   —— .github/workflows/ci.yml 的 integration job 用 docker exec 把本文件灌进 mysql 服务容器
-- 字符集与各服务建表语句一致（utf8mb4 / utf8mb4_0900_ai_ci，即 MySQL 8 服务器默认），否则跨库 JOIN 与排序规则会混用。

-- Phase 3：ecs-service（端口 28084）的库，表结构由 ecs-service/src/main/resources/db/migration 的 Flyway 脚本负责
CREATE DATABASE IF NOT EXISTS zen_ecs CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
