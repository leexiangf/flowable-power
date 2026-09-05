-- M5 增量升级（已有库执行；全新安装请用 docker/mysql/init/01-schema.sql）
USE `power`;

SET NAMES utf8mb4;

-- 任务加签权限
INSERT IGNORE INTO `sys_menu` (
    `id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `perms`,
    `icon`, `sort`, `visible`, `status`, `remark`, `create_by`, `update_by`, `deleted`
) VALUES
(335, 330, '任务加签', 3, NULL, NULL, 'workflow:task:addsign', NULL, 5, 1, 1, NULL, 'system', 'system', 0);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 335),
(2, 335);
