-- M4 增量升级（已有库执行；全新安装请直接用 01_schema.sql + 02_data.sql）
USE `power`;

CREATE TABLE IF NOT EXISTS `wf_cc` (
    `id`                   BIGINT       NOT NULL COMMENT '主键（雪花）',
    `process_instance_id`  VARCHAR(64)  NOT NULL COMMENT '流程实例 ID',
    `task_id`              VARCHAR(64)           DEFAULT NULL COMMENT '关联任务 ID（可空）',
    `user_id`              BIGINT       NOT NULL COMMENT '抄送人用户 ID',
    `from_user_id`         BIGINT       NOT NULL COMMENT '抄送操作人用户 ID',
    `read_flag`            TINYINT      NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
    `create_time`          DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`            VARCHAR(64)           DEFAULT NULL COMMENT '创建人',
    `update_time`          DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by`            VARCHAR(64)           DEFAULT NULL COMMENT '更新人',
    `deleted`              TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_process_instance_id` (`process_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程抄送';

CREATE TABLE IF NOT EXISTS `wf_urge_log` (
    `id`                   BIGINT       NOT NULL COMMENT '主键（雪花）',
    `process_instance_id`  VARCHAR(64)  NOT NULL COMMENT '流程实例 ID',
    `from_user_id`         BIGINT       NOT NULL COMMENT '催办人用户 ID',
    `to_user_id`           BIGINT                DEFAULT NULL COMMENT '指定催办对象（可空=全体待办）',
    `comment`              VARCHAR(500)          DEFAULT NULL COMMENT '催办说明',
    `create_time`          DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`            VARCHAR(64)           DEFAULT NULL COMMENT '创建人',
    `update_time`          DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by`            VARCHAR(64)           DEFAULT NULL COMMENT '更新人',
    `deleted`              TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (`id`),
    KEY `idx_process_instance_id` (`process_instance_id`),
    KEY `idx_from_user_id` (`from_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程催办记录';

-- 菜单与角色权限（INSERT IGNORE 可重复执行）
INSERT IGNORE INTO `sys_menu` (
    `id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `perms`,
    `icon`, `sort`, `visible`, `status`, `remark`, `create_by`, `update_by`, `deleted`
) VALUES
(314, 310, '删除部署', 3, NULL, NULL, 'workflow:definition:remove', NULL, 4, 1, 1, NULL, 'system', 'system', 0),
(324, 320, '强制终止', 3, NULL, NULL, 'workflow:instance:terminate', NULL, 4, 1, 1, NULL, 'system', 'system', 0),
(325, 320, '实例挂起', 3, NULL, NULL, 'workflow:instance:suspend', NULL, 5, 1, 1, NULL, 'system', 'system', 0),
(326, 320, '流程催办', 3, NULL, NULL, 'workflow:task:urge', NULL, 6, 1, 1, NULL, 'system', 'system', 0),
(333, 330, '任务委派', 3, NULL, NULL, 'workflow:task:delegate', NULL, 3, 1, 1, NULL, 'system', 'system', 0),
(334, 330, '抄送查看', 3, NULL, NULL, 'workflow:task:cc', NULL, 4, 1, 1, NULL, 'system', 'system', 0),
(342, 340, '我的请假', 3, NULL, NULL, 'workflow:leave:list', NULL, 2, 1, 1, NULL, 'system', 'system', 0);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
-- ADMIN 增量（已有库升级时补齐；全新种子已通过 SELECT 全量授权）
(1, 314), (1, 324), (1, 325), (1, 326), (1, 333), (1, 334), (1, 342),
(2, 333), (2, 334),
(3, 326), (3, 334), (3, 342);
