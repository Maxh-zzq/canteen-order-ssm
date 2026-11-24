-- =====================================================================
-- 校园食堂网上订餐系统 - 初始化表结构
-- 版本：V1
-- 说明：所有业务表带 deleted 逻辑删除标记（购物车为临时数据，物理删除）
-- =====================================================================

CREATE TABLE IF NOT EXISTS `sys_user` (
    `id`          BIGINT       NOT NULL COMMENT '主键',
    `username`    VARCHAR(32)  NOT NULL COMMENT '学号/工号',
    `password`    VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码',
    `real_name`   VARCHAR(32)  DEFAULT NULL COMMENT '姓名',
    `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `role_code`   VARCHAR(20)  NOT NULL DEFAULT 'STUDENT' COMMENT 'STUDENT/STAFF/ADMIN',
    `balance`     DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '账户余额',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '1 正常 0 禁用',
    `create_time` DATETIME     DEFAULT NULL,
    `update_time` DATETIME     DEFAULT NULL,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `canteen_stall` (
    `id`          BIGINT      NOT NULL COMMENT '主键',
    `name`        VARCHAR(32) NOT NULL COMMENT '档口名称',
    `floor`       VARCHAR(8)  DEFAULT NULL COMMENT '楼层',
    `window_no`   VARCHAR(8)  DEFAULT NULL COMMENT '窗口号',
    `manager_id`  BIGINT      DEFAULT NULL COMMENT '负责人用户 ID',
    `open_time`   VARCHAR(5)  DEFAULT NULL COMMENT '开始营业 HH:mm',
    `close_time`  VARCHAR(5)  DEFAULT NULL COMMENT '结束营业 HH:mm',
    `status`      TINYINT     NOT NULL DEFAULT 1 COMMENT '1 营业 0 停业',
    `create_time` DATETIME    DEFAULT NULL,
    `update_time` DATETIME    DEFAULT NULL,
    `deleted`     TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='食堂档口';

CREATE TABLE IF NOT EXISTS `dish_category` (
    `id`          BIGINT      NOT NULL COMMENT '主键',
    `name`        VARCHAR(32) NOT NULL COMMENT '分类名称',
    `sort`        INT         NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
    `status`      TINYINT     NOT NULL DEFAULT 1 COMMENT '1 启用 0 停用',
    `create_time` DATETIME    DEFAULT NULL,
    `update_time` DATETIME    DEFAULT NULL,
    `deleted`     TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='菜品分类';

CREATE TABLE IF NOT EXISTS `dish` (
    `id`          BIGINT       NOT NULL COMMENT '主键',
    `stall_id`    BIGINT       NOT NULL COMMENT '档口 ID',
    `category_id` BIGINT       NOT NULL COMMENT '分类 ID',
    `name`        VARCHAR(64)  NOT NULL COMMENT '菜品名称',
    `cover_url`   VARCHAR(256) DEFAULT NULL COMMENT '封面图',
    `price`       DECIMAL(10,2) NOT NULL COMMENT '基础价格',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
    `daily_stock` INT          NOT NULL DEFAULT 50 COMMENT '每日默认库存',
    `sales`       INT          NOT NULL DEFAULT 0 COMMENT '累计销量',
    `avg_score`   DECIMAL(3,1) NOT NULL DEFAULT 0.0 COMMENT '平均评分',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '1 上架 0 下架',
    `create_time` DATETIME     DEFAULT NULL,
    `update_time` DATETIME     DEFAULT NULL,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_stall_category` (`stall_id`, `category_id`),
    KEY `idx_status_sales` (`status`, `sales`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='菜品';

CREATE TABLE IF NOT EXISTS `dish_spec` (
    `id`          BIGINT       NOT NULL COMMENT '主键',
    `dish_id`     BIGINT       NOT NULL COMMENT '菜品 ID',
    `name`        VARCHAR(16)  NOT NULL COMMENT '规格名，如大份',
    `price_diff`  DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '差价',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '1 启用 0 停用',
    `create_time` DATETIME     DEFAULT NULL,
    `update_time` DATETIME     DEFAULT NULL,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_dish` (`dish_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='菜品规格';

CREATE TABLE IF NOT EXISTS `daily_menu` (
    `id`          BIGINT   NOT NULL COMMENT '主键',
    `dish_id`     BIGINT   NOT NULL COMMENT '菜品 ID',
    `serve_date`  DATE     NOT NULL COMMENT '供应日期',
    `stock_total` INT      NOT NULL DEFAULT 0 COMMENT '总供应量',
    `stock_left`  INT      NOT NULL DEFAULT 0 COMMENT '剩余库存',
    `sales`       INT      NOT NULL DEFAULT 0 COMMENT '当日已售',
    `status`      TINYINT  NOT NULL DEFAULT 1 COMMENT '1 供应 0 停售',
    `create_time` DATETIME DEFAULT NULL,
    `update_time` DATETIME DEFAULT NULL,
    `deleted`     TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dish_date` (`dish_id`, `serve_date`),
    KEY `idx_date_status` (`serve_date`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='每日菜单与库存';

CREATE TABLE IF NOT EXISTS `cart_item` (
    `id`          BIGINT   NOT NULL COMMENT '主键',
    `user_id`     BIGINT   NOT NULL COMMENT '用户 ID',
    `dish_id`     BIGINT   NOT NULL COMMENT '菜品 ID',
    `spec_id`     BIGINT   NOT NULL DEFAULT 0 COMMENT '规格 ID，0 表示无规格',
    `quantity`    INT      NOT NULL DEFAULT 1 COMMENT '数量',
    `create_time` DATETIME DEFAULT NULL,
    `update_time` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_dish_spec` (`user_id`, `dish_id`, `spec_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='购物车（Redis 兜底）';

CREATE TABLE IF NOT EXISTS `order_info` (
    `id`            BIGINT       NOT NULL COMMENT '主键',
    `order_no`      VARCHAR(32)  NOT NULL COMMENT '订单号',
    `user_id`       BIGINT       NOT NULL COMMENT '下单用户',
    `stall_id`      BIGINT       NOT NULL COMMENT '档口',
    `status`        TINYINT      NOT NULL DEFAULT 10 COMMENT '10待支付 20已支付 30备餐中 40可取餐 50已完成 60已取消 70已退款',
    `total_amount`  DECIMAL(10,2) NOT NULL COMMENT '商品总额',
    `pay_amount`    DECIMAL(10,2) NOT NULL COMMENT '实付金额',
    `pickup_code`   VARCHAR(8)   DEFAULT NULL COMMENT '取餐码',
    `expect_time`   DATETIME     DEFAULT NULL COMMENT '期望取餐时间',
    `expire_time`   DATETIME     DEFAULT NULL COMMENT '支付截止时间',
    `pay_time`      DATETIME     DEFAULT NULL,
    `finish_time`   DATETIME     DEFAULT NULL,
    `cancel_reason` VARCHAR(200) DEFAULT NULL,
    `remark`        VARCHAR(200) DEFAULT NULL,
    `request_id`    VARCHAR(64)  DEFAULT NULL COMMENT '客户端幂等号',
    `version`       INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `create_time`   DATETIME     DEFAULT NULL,
    `update_time`   DATETIME     DEFAULT NULL,
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    UNIQUE KEY `uk_user_request` (`user_id`, `request_id`),
    KEY `idx_user_status` (`user_id`, `status`),
    KEY `idx_stall_status` (`stall_id`, `status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单主表';

CREATE TABLE IF NOT EXISTS `order_item` (
    `id`          BIGINT       NOT NULL COMMENT '主键',
    `order_id`    BIGINT       NOT NULL COMMENT '订单 ID',
    `order_no`    VARCHAR(32)  NOT NULL COMMENT '订单号',
    `dish_id`     BIGINT       NOT NULL COMMENT '菜品 ID',
    `menu_id`     BIGINT       DEFAULT NULL COMMENT '当日菜单 ID，用于回滚库存',
    `dish_name`   VARCHAR(64)  NOT NULL COMMENT '菜品名快照',
    `spec_name`   VARCHAR(16)  DEFAULT NULL COMMENT '规格名快照',
    `price`       DECIMAL(10,2) NOT NULL COMMENT '单价快照',
    `quantity`    INT          NOT NULL COMMENT '数量',
    `amount`      DECIMAL(10,2) NOT NULL COMMENT '小计',
    `create_time` DATETIME     DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_order` (`order_id`),
    KEY `idx_dish` (`dish_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单明细快照';

CREATE TABLE IF NOT EXISTS `order_status_log` (
    `id`            BIGINT      NOT NULL COMMENT '主键',
    `order_id`      BIGINT      NOT NULL,
    `order_no`      VARCHAR(32) NOT NULL,
    `from_status`   TINYINT     DEFAULT NULL COMMENT '变更前状态',
    `to_status`     TINYINT     NOT NULL COMMENT '变更后状态',
    `operator_id`   BIGINT      DEFAULT NULL,
    `operator_name` VARCHAR(32) DEFAULT NULL,
    `remark`        VARCHAR(200) DEFAULT NULL,
    `create_time`   DATETIME    DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单状态流转日志';

CREATE TABLE IF NOT EXISTS `payment_record` (
    `id`            BIGINT       NOT NULL COMMENT '主键',
    `order_no`      VARCHAR(32)  NOT NULL,
    `trade_no`      VARCHAR(64)  NOT NULL COMMENT '支付流水号，唯一',
    `channel`       VARCHAR(20)  NOT NULL COMMENT 'BALANCE/MOCK',
    `amount`        DECIMAL(10,2) NOT NULL,
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '1 成功',
    `callback_time` DATETIME     DEFAULT NULL,
    `create_time`   DATETIME     DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_trade_no` (`trade_no`),
    KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='支付流水';

CREATE TABLE IF NOT EXISTS `refund_record` (
    `id`          BIGINT       NOT NULL COMMENT '主键',
    `order_no`    VARCHAR(32)  NOT NULL,
    `refund_no`   VARCHAR(64)  NOT NULL COMMENT '退款单号，唯一',
    `amount`      DECIMAL(10,2) NOT NULL,
    `status`      TINYINT      NOT NULL DEFAULT 1,
    `reason`      VARCHAR(200) DEFAULT NULL,
    `create_time` DATETIME     DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_refund_no` (`refund_no`),
    KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='退款流水';

CREATE TABLE IF NOT EXISTS `review` (
    `id`          BIGINT       NOT NULL COMMENT '主键',
    `order_id`    BIGINT       NOT NULL,
    `order_no`    VARCHAR(32)  NOT NULL,
    `user_id`     BIGINT       NOT NULL,
    `username`    VARCHAR(32)  DEFAULT NULL COMMENT '展示昵称（脱敏）',
    `stall_id`    BIGINT       DEFAULT NULL,
    `score`       TINYINT      NOT NULL COMMENT '1~5',
    `content`     VARCHAR(300) DEFAULT NULL,
    `images`      VARCHAR(1000) DEFAULT NULL COMMENT '逗号分隔',
    `create_time` DATETIME     DEFAULT NULL,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order` (`order_id`),
    KEY `idx_stall` (`stall_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单评价';

CREATE TABLE IF NOT EXISTS `sys_oper_log` (
    `id`          BIGINT       NOT NULL COMMENT '主键',
    `trace_id`    VARCHAR(32)  DEFAULT NULL,
    `user_id`     BIGINT       DEFAULT NULL,
    `username`    VARCHAR(32)  DEFAULT NULL,
    `module`      VARCHAR(32)  DEFAULT NULL,
    `method`      VARCHAR(128) DEFAULT NULL,
    `params`      TEXT         DEFAULT NULL,
    `ip`          VARCHAR(64)  DEFAULT NULL,
    `cost_ms`     BIGINT       DEFAULT NULL,
    `success`     TINYINT      NOT NULL DEFAULT 1,
    `error_msg`   VARCHAR(500) DEFAULT NULL,
    `create_time` DATETIME     DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='操作日志';

CREATE TABLE IF NOT EXISTS `sys_config` (
    `id`           BIGINT       NOT NULL COMMENT '主键',
    `config_key`   VARCHAR(64)  NOT NULL,
    `config_value` VARCHAR(256) NOT NULL,
    `remark`       VARCHAR(128) DEFAULT NULL,
    `create_time`  DATETIME     DEFAULT NULL,
    `update_time`  DATETIME     DEFAULT NULL,
    `deleted`      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='业务参数配置';
