-- Docker 首次启动全量初始化（仅此一个文件）
-- 含：建库 + 业务表 + 种子数据（用户 / 角色 / 菜单 / 分类）
-- 已有库增量升级脚本见：docker/mysql/upgrade/

CREATE DATABASE IF NOT EXISTS `power`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `power`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `sys_role_menu`;
DROP TABLE IF EXISTS `sys_user_role`;
DROP TABLE IF EXISTS `sys_menu`;
DROP TABLE IF EXISTS `sys_role`;
DROP TABLE IF EXISTS `sys_user`;
DROP TABLE IF EXISTS `sys_outbox`;
DROP TABLE IF EXISTS `wf_urge_log`;
DROP TABLE IF EXISTS `wf_cc`;
DROP TABLE IF EXISTS `wf_model`;
DROP TABLE IF EXISTS `wf_category`;
DROP TABLE IF EXISTS `wf_leave`;

CREATE TABLE `sys_user` (
    `id`            BIGINT       NOT NULL COMMENT '主键（雪花）',
    `username`      VARCHAR(64)  NOT NULL COMMENT '登录名',
    `password`      VARCHAR(100) NOT NULL COMMENT '密码（BCrypt）',
    `nickname`      VARCHAR(64)           DEFAULT NULL COMMENT '昵称',
    `phone`         VARCHAR(20)           DEFAULT NULL COMMENT '手机号',
    `email`         VARCHAR(128)          DEFAULT NULL COMMENT '邮箱',
    `avatar`        VARCHAR(255)          DEFAULT NULL COMMENT '头像 URL',
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0停用 1正常',
    `remark`        VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`   DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`     VARCHAR(64)           DEFAULT NULL COMMENT '创建人',
    `update_time`   DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by`     VARCHAR(64)           DEFAULT NULL COMMENT '更新人',
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_status` (`status`),
    KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户';

CREATE TABLE `sys_role` (
    `id`            BIGINT       NOT NULL COMMENT '主键（雪花）',
    `role_code`     VARCHAR(64)  NOT NULL COMMENT '角色编码（唯一，如 ADMIN）',
    `role_name`     VARCHAR(64)  NOT NULL COMMENT '角色名称',
    `sort`          INT                   DEFAULT 0 COMMENT '排序（升序）',
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0停用 1正常',
    `remark`        VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`   DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`     VARCHAR(64)           DEFAULT NULL COMMENT '创建人',
    `update_time`   DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by`     VARCHAR(64)           DEFAULT NULL COMMENT '更新人',
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统角色';

CREATE TABLE `sys_menu` (
    `id`            BIGINT       NOT NULL COMMENT '主键（雪花）',
    `parent_id`     BIGINT       NOT NULL DEFAULT 0 COMMENT '父节点 ID，根为 0',
    `menu_name`     VARCHAR(64)  NOT NULL COMMENT '菜单/按钮名称',
    `menu_type`     TINYINT      NOT NULL DEFAULT 1 COMMENT '类型：1目录 2菜单 3按钮',
    `path`          VARCHAR(255)          DEFAULT NULL COMMENT '路由 path（前端）',
    `component`     VARCHAR(255)          DEFAULT NULL COMMENT '前端组件路径',
    `perms`         VARCHAR(128)          DEFAULT NULL COMMENT '权限码',
    `icon`          VARCHAR(64)           DEFAULT NULL COMMENT '图标',
    `sort`          INT                   DEFAULT 0 COMMENT '排序（升序）',
    `visible`       TINYINT      NOT NULL DEFAULT 1 COMMENT '是否显示：0隐藏 1显示',
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0停用 1正常',
    `remark`        VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`   DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`     VARCHAR(64)           DEFAULT NULL COMMENT '创建人',
    `update_time`   DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by`     VARCHAR(64)           DEFAULT NULL COMMENT '更新人',
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_perms` (`perms`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单与权限点';

CREATE TABLE `sys_user_role` (
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `role_id` BIGINT NOT NULL COMMENT '角色 ID',
    PRIMARY KEY (`user_id`, `role_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联';

CREATE TABLE `sys_role_menu` (
    `role_id` BIGINT NOT NULL COMMENT '角色 ID',
    `menu_id` BIGINT NOT NULL COMMENT '菜单 ID',
    PRIMARY KEY (`role_id`, `menu_id`),
    KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色菜单关联';

CREATE TABLE `sys_outbox` (
    `id`            BIGINT       NOT NULL COMMENT '主键（雪花）',
    `topic`         VARCHAR(128) NOT NULL COMMENT '交换机 / Topic',
    `tag`           VARCHAR(128)          DEFAULT NULL COMMENT '路由键 / Tag',
    `payload`       TEXT         NOT NULL COMMENT '消息体 JSON',
    `status`        VARCHAR(16)  NOT NULL COMMENT 'NEW / SENT / FAILED',
    `retry_count`   INT                   DEFAULT 0 COMMENT '投递重试次数',
    `create_time`   DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_status_ctime` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='事务出箱消息';

CREATE TABLE `wf_leave` (
    `id`                   BIGINT         NOT NULL COMMENT '主键（雪花）',
    `user_id`              BIGINT         NOT NULL COMMENT '申请人用户 ID',
    `username`             VARCHAR(64)             DEFAULT NULL COMMENT '申请人登录名',
    `days`                 DECIMAL(5,1)   NOT NULL COMMENT '请假天数',
    `reason`               VARCHAR(500)   NOT NULL COMMENT '事由',
    `start_date`           DATE           NOT NULL COMMENT '开始日期',
    `end_date`             DATE           NOT NULL COMMENT '结束日期',
    `status`               TINYINT        NOT NULL DEFAULT 1 COMMENT '1审批中 2通过 3驳回 4撤销',
    `process_instance_id`  VARCHAR(64)             DEFAULT NULL COMMENT '流程实例 ID',
    `create_time`          DATETIME                DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`            VARCHAR(64)             DEFAULT NULL COMMENT '创建人',
    `update_time`          DATETIME                DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by`            VARCHAR(64)             DEFAULT NULL COMMENT '更新人',
    `deleted`              TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_process_instance_id` (`process_instance_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='请假申请';

CREATE TABLE `wf_category` (
    `id`            BIGINT       NOT NULL COMMENT '主键（雪花）',
    `code`          VARCHAR(64)  NOT NULL COMMENT '分类编码',
    `name`          VARCHAR(64)  NOT NULL COMMENT '分类名称',
    `sort`          INT                   DEFAULT 0 COMMENT '排序（升序）',
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0停用 1正常',
    `remark`        VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`   DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`     VARCHAR(64)           DEFAULT NULL COMMENT '创建人',
    `update_time`   DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by`     VARCHAR(64)           DEFAULT NULL COMMENT '更新人',
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程分类';

CREATE TABLE `wf_model` (
    `id`            BIGINT       NOT NULL COMMENT '主键（雪花）',
    `model_key`     VARCHAR(64)  NOT NULL COMMENT '模型 key（部署后作为 processDefinitionKey）',
    `name`          VARCHAR(128) NOT NULL COMMENT '模型名称',
    `category_code` VARCHAR(64)           DEFAULT NULL COMMENT '分类编码',
    `bpmn_xml`      MEDIUMTEXT   NOT NULL COMMENT 'BPMN XML',
    `version`       INT          NOT NULL DEFAULT 1 COMMENT '草稿版本号',
    `remark`        VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`   DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by`     VARCHAR(64)           DEFAULT NULL COMMENT '创建人',
    `update_time`   DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `update_by`     VARCHAR(64)           DEFAULT NULL COMMENT '更新人',
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_model_key` (`model_key`),
    KEY `idx_category_code` (`category_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程模型草稿';

CREATE TABLE `wf_cc` (
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

CREATE TABLE `wf_urge_log` (
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

SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO `sys_user` (
    `id`, `username`, `password`, `nickname`, `email`, `status`, `remark`,
    `create_by`, `update_by`, `deleted`
) VALUES
(1,  'admin',     '$2a$10$1s5S/i9e.8a2az9EHTuxc.UV2SpXfA4qyC5jtc8ybEVrkxbxmH29W',
 '系统管理员', 'admin@power.local', 1, '超级管理员', 'system', 'system', 0),
(2,  'approver1', '$2a$10$1s5S/i9e.8a2az9EHTuxc.UV2SpXfA4qyC5jtc8ybEVrkxbxmH29W',
 '王芳', 'approver1@power.local', 1, '人事经理', 'system', 'system', 0),
(3,  'approver2', '$2a$10$1s5S/i9e.8a2az9EHTuxc.UV2SpXfA4qyC5jtc8ybEVrkxbxmH29W',
 '李明', 'approver2@power.local', 1, '财务经理', 'system', 'system', 0),
(4,  'approver3', '$2a$10$1s5S/i9e.8a2az9EHTuxc.UV2SpXfA4qyC5jtc8ybEVrkxbxmH29W',
 '陈伟', 'approver3@power.local', 1, '技术主管', 'system', 'system', 0),
(5,  'approver4', '$2a$10$1s5S/i9e.8a2az9EHTuxc.UV2SpXfA4qyC5jtc8ybEVrkxbxmH29W',
 '赵云', 'approver4@power.local', 1, '运营主管', 'system', 'system', 0),
(6,  'approver5', '$2a$10$1s5S/i9e.8a2az9EHTuxc.UV2SpXfA4qyC5jtc8ybEVrkxbxmH29W',
 '孙丽', 'approver5@power.local', 1, '行政主管', 'system', 'system', 0),
(7,  'staff1',    '$2a$10$1s5S/i9e.8a2az9EHTuxc.UV2SpXfA4qyC5jtc8ybEVrkxbxmH29W',
 '张伟', 'staff1@power.local', 1, '研发工程师', 'system', 'system', 0),
(8,  'staff2',    '$2a$10$1s5S/i9e.8a2az9EHTuxc.UV2SpXfA4qyC5jtc8ybEVrkxbxmH29W',
 '刘娜', 'staff2@power.local', 1, '产品助理', 'system', 'system', 0),
(9,  'staff3',    '$2a$10$1s5S/i9e.8a2az9EHTuxc.UV2SpXfA4qyC5jtc8ybEVrkxbxmH29W',
 '周杰', 'staff3@power.local', 1, '销售专员', 'system', 'system', 0),
(10, 'staff4',    '$2a$10$1s5S/i9e.8a2az9EHTuxc.UV2SpXfA4qyC5jtc8ybEVrkxbxmH29W',
 '吴欣', 'staff4@power.local', 1, '市场专员', 'system', 'system', 0),
(11, 'staff5',    '$2a$10$1s5S/i9e.8a2az9EHTuxc.UV2SpXfA4qyC5jtc8ybEVrkxbxmH29W',
 '何佳', 'staff5@power.local', 1, '客服专员', 'system', 'system', 0);

INSERT INTO `sys_role` (
    `id`, `role_code`, `role_name`, `sort`, `status`, `remark`,
    `create_by`, `update_by`, `deleted`
) VALUES
(1, 'ADMIN', '超级管理员', 0, 1, '拥有全部管理权限', 'system', 'system', 0),
(2, 'APPROVER', '审批人', 1, 1, '流程候选人组 APPROVER；可被选为会签/审批办理人', 'system', 'system', 0),
(3, 'STAFF', '普通员工', 2, 1, '可发起请假/业务，不可作为审批办理人', 'system', 'system', 0);

INSERT INTO `sys_menu` (
    `id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `perms`,
    `icon`, `sort`, `visible`, `status`, `remark`, `create_by`, `update_by`, `deleted`
) VALUES
(100, 0,   '系统管理', 1, '/system', NULL, NULL, 'setting', 1, 1, 1, NULL, 'system', 'system', 0),
(110, 100, '用户管理', 2, 'user',    'system/user/index', NULL, 'user', 1, 1, 1, NULL, 'system', 'system', 0),
(111, 110, '用户查询', 3, NULL, NULL, 'system:user:list',   NULL, 1, 1, 1, NULL, 'system', 'system', 0),
(112, 110, '用户新增', 3, NULL, NULL, 'system:user:add',    NULL, 2, 1, 1, NULL, 'system', 'system', 0),
(113, 110, '用户修改', 3, NULL, NULL, 'system:user:edit',   NULL, 3, 1, 1, NULL, 'system', 'system', 0),
(114, 110, '用户删除', 3, NULL, NULL, 'system:user:remove', NULL, 4, 1, 1, NULL, 'system', 'system', 0),
(120, 100, '角色管理', 2, 'role', 'system/role/index', NULL, 'peoples', 2, 1, 1, NULL, 'system', 'system', 0),
(121, 120, '角色查询', 3, NULL, NULL, 'system:role:list',   NULL, 1, 1, 1, NULL, 'system', 'system', 0),
(122, 120, '角色新增', 3, NULL, NULL, 'system:role:add',    NULL, 2, 1, 1, NULL, 'system', 'system', 0),
(123, 120, '角色修改', 3, NULL, NULL, 'system:role:edit',   NULL, 3, 1, 1, NULL, 'system', 'system', 0),
(124, 120, '角色删除', 3, NULL, NULL, 'system:role:remove', NULL, 4, 1, 1, NULL, 'system', 'system', 0),
(130, 100, '菜单管理', 2, 'menu', 'system/menu/index', NULL, 'tree-table', 3, 1, 1, NULL, 'system', 'system', 0),
(131, 130, '菜单查询', 3, NULL, NULL, 'system:menu:list',   NULL, 1, 1, 1, NULL, 'system', 'system', 0),
(132, 130, '菜单新增', 3, NULL, NULL, 'system:menu:add',    NULL, 2, 1, 1, NULL, 'system', 'system', 0),
(133, 130, '菜单修改', 3, NULL, NULL, 'system:menu:edit',   NULL, 3, 1, 1, NULL, 'system', 'system', 0),
(134, 130, '菜单删除', 3, NULL, NULL, 'system:menu:remove', NULL, 4, 1, 1, NULL, 'system', 'system', 0),
(200, 0,   '系统探针', 1, '/probe', NULL, NULL, 'example', 2, 1, 1, NULL, 'system', 'system', 0),
(210, 200, '联调接口', 2, 'ping',  'system/probe/index', NULL, 'guide', 1, 1, 1, NULL, 'system', 'system', 0),
(211, 210, 'Ping权限', 3, NULL, NULL, 'system:probe:ping', NULL, 1, 1, 1, '对应 /system/ping', 'system', 'system', 0),
(300, 0,   '工作流', 1, '/workflow', NULL, NULL, 'tree', 3, 1, 1, NULL, 'system', 'system', 0),
(310, 300, '流程定义', 2, 'definition', 'workflow/definition/index', NULL, 'guide', 1, 1, 1, NULL, 'system', 'system', 0),
(311, 310, '定义查询', 3, NULL, NULL, 'workflow:definition:list', NULL, 1, 1, 1, NULL, 'system', 'system', 0),
(312, 310, '流程部署', 3, NULL, NULL, 'workflow:definition:deploy', NULL, 2, 1, 1, NULL, 'system', 'system', 0),
(313, 310, '挂起激活', 3, NULL, NULL, 'workflow:definition:suspend', NULL, 3, 1, 1, NULL, 'system', 'system', 0),
(314, 310, '删除部署', 3, NULL, NULL, 'workflow:definition:remove', NULL, 4, 1, 1, NULL, 'system', 'system', 0),
(320, 300, '流程实例', 2, 'instance', 'workflow/instance/index', NULL, 'list', 2, 1, 1, NULL, 'system', 'system', 0),
(321, 320, '实例查询', 3, NULL, NULL, 'workflow:instance:list', NULL, 1, 1, 1, NULL, 'system', 'system', 0),
(322, 320, '发起流程', 3, NULL, NULL, 'workflow:instance:start', NULL, 2, 1, 1, NULL, 'system', 'system', 0),
(323, 320, '实例监控', 3, NULL, NULL, 'workflow:instance:monitor', NULL, 3, 1, 1, NULL, 'system', 'system', 0),
(324, 320, '强制终止', 3, NULL, NULL, 'workflow:instance:terminate', NULL, 4, 1, 1, NULL, 'system', 'system', 0),
(325, 320, '实例挂起', 3, NULL, NULL, 'workflow:instance:suspend', NULL, 5, 1, 1, NULL, 'system', 'system', 0),
(326, 320, '流程催办', 3, NULL, NULL, 'workflow:task:urge', NULL, 6, 1, 1, NULL, 'system', 'system', 0),
(330, 300, '任务中心', 2, 'task', 'workflow/task/index', NULL, 'edit', 3, 1, 1, NULL, 'system', 'system', 0),
(331, 330, '任务查询', 3, NULL, NULL, 'workflow:task:list', NULL, 1, 1, 1, NULL, 'system', 'system', 0),
(332, 330, '任务办理', 3, NULL, NULL, 'workflow:task:handle', NULL, 2, 1, 1, NULL, 'system', 'system', 0),
(333, 330, '任务委派', 3, NULL, NULL, 'workflow:task:delegate', NULL, 3, 1, 1, NULL, 'system', 'system', 0),
(334, 330, '抄送查看', 3, NULL, NULL, 'workflow:task:cc', NULL, 4, 1, 1, NULL, 'system', 'system', 0),
(335, 330, '任务加签', 3, NULL, NULL, 'workflow:task:addsign', NULL, 5, 1, 1, NULL, 'system', 'system', 0),
(340, 300, '请假管理', 2, 'leave', 'workflow/leave/index', NULL, 'form', 4, 1, 1, NULL, 'system', 'system', 0),
(341, 340, '请假申请', 3, NULL, NULL, 'workflow:leave:apply', NULL, 1, 1, 1, '对应 /workflow/leave', 'system', 'system', 0),
(342, 340, '我的请假', 3, NULL, NULL, 'workflow:leave:list', NULL, 2, 1, 1, NULL, 'system', 'system', 0),
(350, 300, '流程分类', 2, 'category', 'workflow/category/index', NULL, 'tree-table', 5, 1, 1, NULL, 'system', 'system', 0),
(351, 350, '分类查询', 3, NULL, NULL, 'workflow:category:list', NULL, 1, 1, 1, NULL, 'system', 'system', 0),
(352, 350, '分类编辑', 3, NULL, NULL, 'workflow:category:edit', NULL, 2, 1, 1, NULL, 'system', 'system', 0),
(360, 300, '流程模型', 2, 'model', 'workflow/model/index', NULL, 'build', 6, 1, 1, NULL, 'system', 'system', 0),
(361, 360, '模型查询', 3, NULL, NULL, 'workflow:model:list', NULL, 1, 1, 1, NULL, 'system', 'system', 0),
(362, 360, '模型编辑', 3, NULL, NULL, 'workflow:model:edit', NULL, 2, 1, 1, NULL, 'system', 'system', 0);

INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(1, 1),
(2, 2), (3, 2), (4, 2), (5, 2), (6, 2),
(7, 3), (8, 3), (9, 3), (10, 3), (11, 3);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `id` FROM `sys_menu` WHERE `deleted` = 0;

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(2, 111),
(2, 211),
(2, 311),
(2, 321),
(2, 326),
(2, 331),
(2, 332),
(2, 333),
(2, 334),
(2, 335),
(2, 341),
(2, 342),
(3, 111),
(3, 321),
(3, 322),
(3, 326),
(3, 331),
(3, 332),
(3, 334),
(3, 341),
(3, 342);

INSERT INTO `wf_category` (
    `id`, `code`, `name`, `sort`, `status`, `remark`, `create_by`, `update_by`, `deleted`
) VALUES
(1, 'HR', '人事行政', 1, 1, '请假等人事流程', 'system', 'system', 0),
(2, 'OA', '综合办公', 2, 1, '通用办公流程', 'system', 'system', 0);
