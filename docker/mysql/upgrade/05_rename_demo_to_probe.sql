-- 去 demo 命名：菜单路径 / 权限码迁移（已有库执行）
USE `power`;

SET NAMES utf8mb4;

UPDATE `sys_menu`
SET `menu_name` = '系统探针',
    `path` = '/probe'
WHERE `id` = 200;

UPDATE `sys_menu`
SET `menu_name` = '联调接口',
    `component` = 'system/probe/index'
WHERE `id` = 210;

UPDATE `sys_menu`
SET `perms` = 'system:probe:ping',
    `remark` = '对应 /system/ping'
WHERE `id` = 211 AND (`perms` = 'system:demo:ping' OR `perms` IS NULL OR `perms` <> 'system:probe:ping');

-- 兼容：若仍是旧权限码则强制更新
UPDATE `sys_menu`
SET `perms` = 'system:probe:ping'
WHERE `id` = 211 AND `perms` = 'system:demo:ping';
