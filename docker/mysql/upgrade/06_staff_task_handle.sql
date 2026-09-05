-- STAFF 补齐任务办理权限：被指定为会签/办理人时需能 complete/reject
USE `power`;

SET NAMES utf8mb4;

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 3, 332
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 3 AND `menu_id` = 332
);
