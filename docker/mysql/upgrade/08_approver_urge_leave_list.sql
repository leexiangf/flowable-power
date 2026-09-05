-- APPROVER 补齐：催办 + 我的请假列表
USE `power`;

SET NAMES utf8mb4;

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 2, 326 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 2 AND `menu_id` = 326);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 2, 342 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 2 AND `menu_id` = 342);
