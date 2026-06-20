USE `campus_hub`;
SET NAMES utf8mb4;

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
