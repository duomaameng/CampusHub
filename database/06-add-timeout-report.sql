USE `campus_hub`;

SET NAMES utf8mb4;

ALTER TABLE `report`
  ADD COLUMN `related_order_id` BIGINT DEFAULT NULL COMMENT 'related order id for order-scoped reports' AFTER `target_id`,
  ADD KEY `idx_report_related_order` (`related_order_id`, `reason_type`, `status`),
  ADD CONSTRAINT `fk_report_related_order` FOREIGN KEY (`related_order_id`) REFERENCES `orders` (`id`);
