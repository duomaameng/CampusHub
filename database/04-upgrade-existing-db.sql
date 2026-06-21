USE `campus_hub`;
SET NAMES utf8mb4;

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

-- Keep the current demo task-image assignment.
DELETE FROM `task_image` WHERE `task_id` IN (2, 4);
INSERT INTO `task_image` (`task_id`, `image_url`, `sort_order`)
VALUES (2, '/uploads/image_editor_1781931903418..jpg', 1);
