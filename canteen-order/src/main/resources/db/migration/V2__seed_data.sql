-- =====================================================================
-- 校园食堂网上订餐系统 - 演示数据
-- 版本：V2
-- 账号由 DataInitializer 在启动时用 BCrypt 生成
-- =====================================================================

INSERT INTO `canteen_stall` (`id`, `name`, `floor`, `window_no`, `open_time`, `close_time`, `status`, `create_time`, `update_time`, `deleted`) VALUES
(1, '一食堂·家常菜', '1F', '8号窗口', '07:00', '20:00', 1, NOW(), NOW(), 0),
(2, '二食堂·面食馆', '2F', '3号窗口', '07:00', '20:30', 1, NOW(), NOW(), 0),
(3, '三食堂·轻食吧', '1F', '5号窗口', '06:30', '21:00', 1, NOW(), NOW(), 0);

INSERT INTO `dish_category` (`id`, `name`, `sort`, `status`, `create_time`, `update_time`, `deleted`) VALUES
(1, '盖浇饭', 1, 1, NOW(), NOW(), 0),
(2, '面食', 2, 1, NOW(), NOW(), 0),
(3, '汤粥', 3, 1, NOW(), NOW(), 0),
(4, '小吃饮品', 4, 1, NOW(), NOW(), 0);

INSERT INTO `dish` (`id`, `stall_id`, `category_id`, `name`, `cover_url`, `price`, `description`, `daily_stock`, `sales`, `avg_score`, `status`, `create_time`, `update_time`, `deleted`) VALUES
(101, 1, 1, '番茄炒蛋盖饭', NULL, 12.00, '现炒番茄鸡蛋，配米饭和时蔬', 60, 0, 0.0, 1, NOW(), NOW(), 0),
(102, 1, 1, '宫保鸡丁盖饭', NULL, 14.00, '鸡腿肉、花生米，微辣', 50, 0, 0.0, 1, NOW(), NOW(), 0),
(103, 1, 1, '土豆牛腩饭', NULL, 16.00, '慢炖牛腩配土豆，软烂入味', 40, 0, 0.0, 1, NOW(), NOW(), 0),
(104, 2, 2, '红烧牛肉面', NULL, 15.00, '牛骨汤底，手工拉面', 50, 0, 0.0, 1, NOW(), NOW(), 0),
(105, 2, 2, '油泼面', NULL, 11.00, '宽面配辣椒面，热油现泼', 60, 0, 0.0, 1, NOW(), NOW(), 0),
(106, 2, 2, '香菇鸡丝面', NULL, 12.00, '清汤，香菇与鸡丝', 40, 0, 0.0, 1, NOW(), NOW(), 0),
(107, 3, 3, '皮蛋瘦肉粥', NULL, 6.00, '熬煮两小时，配咸鸭蛋可选', 80, 0, 0.0, 1, NOW(), NOW(), 0),
(108, 3, 3, '南瓜小米粥', NULL, 5.00, '南瓜自然甜，无添加糖', 60, 0, 0.0, 1, NOW(), NOW(), 0),
(109, 3, 4, '手抓饼', NULL, 7.00, '现煎千层饼，可加蛋加肠', 50, 0, 0.0, 1, NOW(), NOW(), 0),
(110, 3, 4, '柠檬茶', NULL, 5.00, '鲜柠檬，少冰可选', 100, 0, 0.0, 1, NOW(), NOW(), 0);

INSERT INTO `dish_spec` (`id`, `dish_id`, `name`, `price_diff`, `status`, `create_time`, `update_time`, `deleted`) VALUES
(1, 101, '小份', 0.00, 1, NOW(), NOW(), 0),
(2, 101, '大份', 3.00, 1, NOW(), NOW(), 0),
(3, 104, '小碗', 0.00, 1, NOW(), NOW(), 0),
(4, 104, '大碗', 4.00, 1, NOW(), NOW(), 0),
(5, 109, '加蛋', 2.00, 1, NOW(), NOW(), 0);

INSERT INTO `sys_config` (`id`, `config_key`, `config_value`, `remark`, `create_time`, `update_time`, `deleted`) VALUES
(1, 'business.hours', '07:00-20:30', '食堂营业时间', NOW(), NOW(), 0),
(2, 'system.notice', '欢迎使用校园食堂网上订餐系统，下单后 15 分钟内完成支付', '首页公告', NOW(), NOW(), 0);
