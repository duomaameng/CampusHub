USE `campus_hub`;
SET NAMES utf8mb4;

-- Add payment method for reward tasks.
SET @has_task_payment_method = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'task' AND COLUMN_NAME = 'payment_method'
);
SET @sql = IF(
  @has_task_payment_method = 0,
  'ALTER TABLE `task` ADD COLUMN `payment_method` VARCHAR(16) DEFAULT NULL COMMENT ''WECHAT ALIPAY or CASH when reward type is CASH'' AFTER `reward_amount`',
  'SELECT 1'
);
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @has_publisher_viewed = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'application' AND COLUMN_NAME = 'publisher_viewed'
);
SET @sql = IF(
  @has_publisher_viewed = 0,
  'ALTER TABLE `application` ADD COLUMN `publisher_viewed` TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''whether publisher has viewed this application'' AFTER `status`',
  'SELECT 1'
);
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

UPDATE `task`
SET `payment_method` = 'CASH'
WHERE `id` > 0
  AND `reward_type` = 'CASH'
  AND `payment_method` IS NULL;

-- Add file-message support to an existing order_message table.
SET @has_message_file_id = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_message' AND COLUMN_NAME = 'file_id'
);
SET @sql = IF(
  @has_message_file_id = 0,
  'ALTER TABLE `order_message` ADD COLUMN `file_id` BIGINT DEFAULT NULL COMMENT ''chat attachment file record id'' AFTER `image_url`',
  'SELECT 1'
);
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @has_message_file_index = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_message' AND INDEX_NAME = 'idx_order_message_file_id'
);
SET @sql = IF(
  @has_message_file_index = 0,
  'ALTER TABLE `order_message` ADD KEY `idx_order_message_file_id` (`file_id`)',
  'SELECT 1'
);
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @has_message_file_fk = (
  SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'order_message' AND CONSTRAINT_NAME = 'fk_order_message_file'
);
SET @sql = IF(
  @has_message_file_fk = 0,
  'ALTER TABLE `order_message` ADD CONSTRAINT `fk_order_message_file` FOREIGN KEY (`file_id`) REFERENCES `file_record` (`id`)',
  'SELECT 1'
);
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

-- Add task-file relations for files uploaded while publishing a task.
CREATE TABLE IF NOT EXISTS `task_file` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'task file relation id',
  `task_id` BIGINT NOT NULL COMMENT 'task id',
  `file_record_id` BIGINT NOT NULL COMMENT 'file record id',
  `sort_order` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_task_file_task_id` (`task_id`),
  KEY `idx_task_file_record_id` (`file_record_id`),
  CONSTRAINT `fk_task_file_task` FOREIGN KEY (`task_id`) REFERENCES `task` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_task_file_record` FOREIGN KEY (`file_record_id`) REFERENCES `file_record` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='task file relation table';

-- Normalize legacy credit data to the current 0-100 range.
UPDATE `credit_log`
SET `score_before` = LEAST(100, GREATEST(0, `score_before`)),
    `score_after` = LEAST(100, GREATEST(0, `score_after`))
WHERE `id` > 0;

UPDATE `credit_log`
SET `change_amount` = `score_after` - `score_before`
WHERE `id` > 0;

-- Add order-scoped report support only when upgrading an older database.
SET @has_related_order_column = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report' AND COLUMN_NAME = 'related_order_id'
);
SET @sql = IF(
  @has_related_order_column = 0,
  'ALTER TABLE `report` ADD COLUMN `related_order_id` BIGINT DEFAULT NULL COMMENT ''related order id for order-scoped reports'' AFTER `target_id`',
  'SELECT 1'
);
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @has_related_order_index = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report' AND INDEX_NAME = 'idx_report_related_order'
);
SET @sql = IF(
  @has_related_order_index = 0,
  'ALTER TABLE `report` ADD KEY `idx_report_related_order` (`related_order_id`, `reason_type`, `status`)',
  'SELECT 1'
);
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @has_related_order_fk = (
  SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'report' AND CONSTRAINT_NAME = 'fk_report_related_order'
);
SET @sql = IF(
  @has_related_order_fk = 0,
  'ALTER TABLE `report` ADD CONSTRAINT `fk_report_related_order` FOREIGN KEY (`related_order_id`) REFERENCES `orders` (`id`)',
  'SELECT 1'
);
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

-- Keep the current demo task-image assignment when the demo task exists.
INSERT INTO `task_image` (`task_id`, `image_url`, `sort_order`)
SELECT `task`.`id`, '/uploads/image_editor_1781931903418..jpg', 1
FROM `task`
LEFT JOIN `task_image` AS `existing_image`
  ON `existing_image`.`task_id` = `task`.`id`
  AND `existing_image`.`image_url` = '/uploads/image_editor_1781931903418..jpg'
WHERE `task`.`id` = 2
  AND `existing_image`.`id` IS NULL;

-- Add six open demo tasks without duplicating them on repeated upgrades.
INSERT INTO `task` (`publisher_id`, `category`, `title`, `description`, `campus`, `location_detail`, `reward_type`, `reward_amount`, `payment_method`, `deadline`, `status`, `anonymous`, `category_fields`, `version`)
SELECT
  `publisher`.`id`,
  `seed`.`category`,
  `seed`.`title`,
  `seed`.`description`,
  `seed`.`campus`,
  `seed`.`location_detail`,
  'CASH',
  `seed`.`reward_amount`,
  `seed`.`payment_method`,
  `seed`.`deadline`,
  'OPEN',
  `seed`.`anonymous`,
  `seed`.`category_fields`,
  0
FROM (
  SELECT 'student.demo1@smail.nju.edu.cn' AS `publisher_email`, 'SECOND_HAND' AS `category`, '转让九成新雅迪电瓶车' AS `title`, '车况良好，日常通勤使用，刹车和灯光正常，续航约 30 公里。因毕业离校转让，可在校内当面看车试骑。' AS `description`, '仙林校区' AS `campus`, '仙林校区西门附近' AS `location_detail`, 1280.00 AS `reward_amount`, 'ALIPAY' AS `payment_method`, '2026-09-15 20:00:00' AS `deadline`, 0 AS `anonymous`, JSON_OBJECT('goodsCategory', '电动车', 'condition', 'LIKE_NEW') AS `category_fields`
  UNION ALL
  SELECT 'student.demo2@smail.nju.edu.cn', 'SECOND_HAND', '转让变速山地自行车', '车架结实，变速和刹车功能正常，适合校园通勤和周末骑行，外观有少量正常使用痕迹。', '仙林校区', '仙林校区体育馆附近', 520.00, 'WECHAT', '2026-09-20 19:30:00', 0, JSON_OBJECT('goodsCategory', '自行车', 'condition', 'USED')
  UNION ALL
  SELECT 'student.demo1@smail.nju.edu.cn', 'EXPRESS', '晚上帮取一个大件快递', '快递已经到菜鸟驿站，箱子稍大，希望今晚帮忙取出并送到宿舍楼下。', '仙林校区', '菜鸟驿站至九舍', 10.00, 'WECHAT', '2026-09-06 21:00:00', 0, JSON_OBJECT('expressCompany', '京东快递', 'pickupLocation', '菜鸟驿站', 'pickupCode', '接单后告知', 'deliveryLocation', '九舍楼下')
  UNION ALL
  SELECT 'student.demo2@smail.nju.edu.cn', 'TUTORING', '求一次高数重点题型辅导', '准备补考，希望找一位高数基础扎实的同学讲解极限、导数和积分重点题型，预计辅导约一个半小时。', '鼓楼校区', '鼓楼校区教学楼或线上', 80.00, 'ALIPAY', '2026-09-18 18:00:00', 0, JSON_OBJECT('subject', '高等数学', 'level', '本科基础')
  UNION ALL
  SELECT 'student.demo1@smail.nju.edu.cn', 'ERRAND', '帮忙搬两箱书到新宿舍', '换宿舍有两箱教材和生活用品需要搬运，距离不远，希望有小推车或力气较大的同学帮忙。', '仙林校区', '十舍至十二舍', 35.00, 'CASH', '2026-09-28 17:30:00', 0, JSON_OBJECT('itemCount', 2, 'estimatedTime', '约 30 分钟')
  UNION ALL
  SELECT 'student.demo2@smail.nju.edu.cn', 'CONSULTATION', '求助修改秋招简历', '软件开发方向秋招简历已经完成初稿，希望有相关求职经验的同学帮忙调整项目描述、排版和重点表达。', '苏州校区', '线上沟通', 40.00, 'WECHAT', '2026-10-08 21:00:00', 1, JSON_OBJECT('topic', '秋招简历修改', 'communication', '线上')
) AS `seed`
JOIN `user` AS `publisher` ON `publisher`.`email` = `seed`.`publisher_email`
LEFT JOIN `task` AS `existing`
  ON `existing`.`publisher_id` = `publisher`.`id`
  AND `existing`.`title` = `seed`.`title`
WHERE `existing`.`id` IS NULL;

INSERT INTO `task_image` (`task_id`, `image_url`, `sort_order`)
SELECT `task`.`id`, '/uploads/image_editor_1782620477906..jpg', 1
FROM `task`
JOIN `user` AS `publisher` ON `publisher`.`id` = `task`.`publisher_id`
LEFT JOIN `task_image` AS `existing_image`
  ON `existing_image`.`task_id` = `task`.`id`
  AND `existing_image`.`image_url` = '/uploads/image_editor_1782620477906..jpg'
WHERE `publisher`.`email` = 'student.demo1@smail.nju.edu.cn'
  AND `task`.`title` = '转让九成新雅迪电瓶车'
  AND `existing_image`.`id` IS NULL;

INSERT INTO `task_image` (`task_id`, `image_url`, `sort_order`)
SELECT `task`.`id`, '/uploads/image_editor_1782620508030..jpg', 1
FROM `task`
JOIN `user` AS `publisher` ON `publisher`.`id` = `task`.`publisher_id`
LEFT JOIN `task_image` AS `existing_image`
  ON `existing_image`.`task_id` = `task`.`id`
  AND `existing_image`.`image_url` = '/uploads/image_editor_1782620508030..jpg'
WHERE `publisher`.`email` = 'student.demo2@smail.nju.edu.cn'
  AND `task`.`title` = '转让变速山地自行车'
  AND `existing_image`.`id` IS NULL;
