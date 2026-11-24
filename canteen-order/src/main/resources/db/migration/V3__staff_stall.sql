-- =====================================================================
-- 员工档口数据权限：一个员工可绑定多个档口，支持停用与生效时间
-- 版本：V3
-- 说明：角色权限决定“能做什么”，本表决定 STAFF “能做哪些档口的数据”
-- =====================================================================

CREATE TABLE IF NOT EXISTS `staff_stall` (
    `id`             BIGINT      NOT NULL COMMENT '主键',
    `user_id`        BIGINT      NOT NULL COMMENT '员工用户 ID',
    `stall_id`       BIGINT      NOT NULL COMMENT '档口 ID',
    `status`         TINYINT     NOT NULL DEFAULT 1 COMMENT '1 有效 0 停用',
    `effective_from` DATETIME    DEFAULT NULL COMMENT '生效时间，空表示立即生效',
    `effective_to`   DATETIME    DEFAULT NULL COMMENT '失效时间，空表示长期有效',
    `create_time`    DATETIME    DEFAULT NULL,
    `update_time`    DATETIME    DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_stall` (`user_id`, `stall_id`),
    KEY `idx_user_status` (`user_id`, `status`),
    KEY `idx_stall_status` (`stall_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='员工档口绑定';
