USE `campus_hub`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
SET SQL_SAFE_UPDATES = 0;

DELETE FROM `admin_operation_log`;
DELETE FROM `announcement`;
DELETE FROM `report_evidence`;
DELETE FROM `task_file`;
DELETE FROM `file_record`;
DELETE FROM `report`;
DELETE FROM `credit_log`;
DELETE FROM `review`;
DELETE FROM `notification`;
DELETE FROM `order_message`;
DELETE FROM `order_status_log`;
DELETE FROM `orders`;
DELETE FROM `application`;
DELETE FROM `favorite`;
DELETE FROM `task_image`;
DELETE FROM `task`;
DELETE FROM `verification_code`;
DELETE FROM `user_profile`;
DELETE FROM `user`;

SET SQL_SAFE_UPDATES = 1;
SET FOREIGN_KEY_CHECKS = 1;
