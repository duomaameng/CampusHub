CREATE DATABASE IF NOT EXISTS `campus_hub`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `campus_hub`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `admin_operation_log`;
DROP TABLE IF EXISTS `announcement`;
DROP TABLE IF EXISTS `report_evidence`;
DROP TABLE IF EXISTS `file_record`;
DROP TABLE IF EXISTS `report`;
DROP TABLE IF EXISTS `credit_log`;
DROP TABLE IF EXISTS `review`;
DROP TABLE IF EXISTS `notification`;
DROP TABLE IF EXISTS `order_message`;
DROP TABLE IF EXISTS `order_status_log`;
DROP TABLE IF EXISTS `orders`;
DROP TABLE IF EXISTS `application`;
DROP TABLE IF EXISTS `favorite`;
DROP TABLE IF EXISTS `task_image`;
DROP TABLE IF EXISTS `task`;
DROP TABLE IF EXISTS `verification_code`;
DROP TABLE IF EXISTS `user_profile`;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'user id',
  `email` VARCHAR(128) NOT NULL COMMENT 'school email',
  `password_hash` VARCHAR(255) NOT NULL COMMENT 'bcrypt password hash',
  `role` VARCHAR(16) NOT NULL DEFAULT 'STUDENT' COMMENT 'STUDENT or ADMIN',
  `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE DISABLED or ANONYMIZED',
  `verified` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'verified flag',
  `student_no_masked` VARCHAR(32) DEFAULT NULL COMMENT 'masked student number',
  `login_failures` INT NOT NULL DEFAULT 0 COMMENT 'continuous login failures',
  `locked_until` DATETIME DEFAULT NULL COMMENT 'account lock end time',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_email` (`email`),
  KEY `idx_user_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='user account table';

CREATE TABLE `user_profile` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'profile id',
  `user_id` BIGINT NOT NULL COMMENT 'user id',
  `nickname` VARCHAR(30) NOT NULL COMMENT 'nickname',
  `avatar_url` VARCHAR(512) DEFAULT NULL COMMENT 'avatar url',
  `gender` VARCHAR(8) DEFAULT NULL COMMENT 'gender',
  `grade` VARCHAR(16) DEFAULT NULL COMMENT 'grade',
  `college` VARCHAR(64) DEFAULT NULL COMMENT 'college',
  `bio` VARCHAR(200) DEFAULT NULL COMMENT 'bio',
  `campus` VARCHAR(32) DEFAULT NULL COMMENT 'default campus',
  `contact` VARCHAR(100) DEFAULT NULL COMMENT 'contact info',
  `contact_visible` TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'contact visible flag',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_profile_user_id` (`user_id`),
  CONSTRAINT `fk_profile_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='user profile table';

CREATE TABLE `verification_code` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'verification code id',
  `email` VARCHAR(128) NOT NULL COMMENT 'target email',
  `code` VARCHAR(6) NOT NULL COMMENT '6 digit code',
  `purpose` VARCHAR(20) NOT NULL COMMENT 'REGISTER or RESET_PASSWORD',
  `expires_at` DATETIME NOT NULL COMMENT 'expire time',
  `used` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'used flag',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_vcode_email_code` (`email`, `code`),
  KEY `idx_vcode_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='email verification code table';

CREATE TABLE `task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'task id',
  `publisher_id` BIGINT NOT NULL COMMENT 'publisher user id',
  `category` VARCHAR(32) NOT NULL COMMENT 'task category',
  `title` VARCHAR(100) NOT NULL COMMENT 'task title',
  `description` VARCHAR(2000) NOT NULL COMMENT 'task description',
  `campus` VARCHAR(32) NOT NULL COMMENT 'campus',
  `location_detail` VARCHAR(128) DEFAULT NULL COMMENT 'location detail',
  `reward_type` VARCHAR(16) NOT NULL COMMENT 'reward type',
  `reward_amount` DECIMAL(10,2) DEFAULT NULL COMMENT 'reward amount',
  `deadline` DATETIME NOT NULL COMMENT 'deadline',
  `status` VARCHAR(20) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN IN_PROGRESS COMPLETED CANCELLED or EXPIRED',
  `anonymous` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'anonymous flag',
  `category_fields` JSON DEFAULT NULL COMMENT 'category specific fields',
  `version` INT NOT NULL DEFAULT 0 COMMENT 'optimistic lock version',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_task_publisher` (`publisher_id`),
  KEY `idx_task_hall` (`status`, `category`, `campus`),
  KEY `idx_task_created_at` (`status`, `created_at`),
  KEY `idx_task_deadline` (`status`, `deadline`),
  CONSTRAINT `fk_task_publisher` FOREIGN KEY (`publisher_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='task table';

CREATE TABLE `task_image` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'task image id',
  `task_id` BIGINT NOT NULL COMMENT 'task id',
  `image_url` VARCHAR(512) NOT NULL COMMENT 'image url',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT 'sort order',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_task_image_task_id` (`task_id`),
  CONSTRAINT `fk_task_image_task` FOREIGN KEY (`task_id`) REFERENCES `task` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='task image table';

CREATE TABLE `favorite` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'favorite id',
  `user_id` BIGINT NOT NULL COMMENT 'user id',
  `task_id` BIGINT NOT NULL COMMENT 'task id',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_favorite_user_task` (`user_id`, `task_id`),
  KEY `idx_favorite_task_id` (`task_id`),
  CONSTRAINT `fk_favorite_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_favorite_task` FOREIGN KEY (`task_id`) REFERENCES `task` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='favorite table';

CREATE TABLE `application` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'application id',
  `task_id` BIGINT NOT NULL COMMENT 'task id',
  `applicant_id` BIGINT NOT NULL COMMENT 'applicant user id',
  `message` VARCHAR(500) NOT NULL COMMENT 'application message',
  `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING APPROVED REJECTED or CANCELLED',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_application_task_status` (`task_id`, `status`),
  KEY `idx_application_applicant_id` (`applicant_id`),
  CONSTRAINT `fk_application_task` FOREIGN KEY (`task_id`) REFERENCES `task` (`id`),
  CONSTRAINT `fk_application_user` FOREIGN KEY (`applicant_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='application table';

CREATE TABLE `orders` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'order id',
  `task_id` BIGINT NOT NULL COMMENT 'task id',
  `publisher_id` BIGINT NOT NULL COMMENT 'publisher user id',
  `service_provider_id` BIGINT DEFAULT NULL COMMENT 'service provider user id; nullable when order is returned to pending confirmation',
  `status` VARCHAR(24) NOT NULL DEFAULT 'PENDING_CONFIRM' COMMENT 'order status',
  `completion_proof_url` VARCHAR(512) DEFAULT NULL COMMENT 'completion proof url',
  `cancel_reason` VARCHAR(500) DEFAULT NULL COMMENT 'cancel reason',
  `version` INT NOT NULL DEFAULT 0 COMMENT 'optimistic lock version',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_task_id` (`task_id`),
  KEY `idx_order_publisher_status` (`publisher_id`, `status`),
  KEY `idx_order_provider_status` (`service_provider_id`, `status`),
  CONSTRAINT `fk_order_task` FOREIGN KEY (`task_id`) REFERENCES `task` (`id`),
  CONSTRAINT `fk_order_publisher` FOREIGN KEY (`publisher_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_order_provider` FOREIGN KEY (`service_provider_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='orders table';

CREATE TABLE `order_status_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'order status log id',
  `order_id` BIGINT NOT NULL COMMENT 'order id',
  `from_status` VARCHAR(24) NOT NULL COMMENT 'status before change',
  `to_status` VARCHAR(24) NOT NULL COMMENT 'status after change',
  `operator_id` BIGINT NOT NULL COMMENT 'operator user id',
  `reason` VARCHAR(500) DEFAULT NULL COMMENT 'change reason',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_osl_order_time` (`order_id`, `created_at`),
  CONSTRAINT `fk_osl_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_osl_operator` FOREIGN KEY (`operator_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='order status log table';

CREATE TABLE `order_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'order message id',
  `order_id` BIGINT NOT NULL COMMENT 'order id',
  `sender_id` BIGINT NOT NULL COMMENT 'sender user id',
  `message_type` VARCHAR(8) NOT NULL COMMENT 'TEXT or IMAGE',
  `content` VARCHAR(2000) DEFAULT NULL COMMENT 'text content',
  `image_url` VARCHAR(512) DEFAULT NULL COMMENT 'image url',
  `is_read` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'read flag',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order_message_order_time` (`order_id`, `created_at`),
  CONSTRAINT `fk_order_message_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_order_message_sender` FOREIGN KEY (`sender_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='order message table';

CREATE TABLE `notification` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'notification id',
  `receiver_id` BIGINT NOT NULL COMMENT 'receiver user id',
  `type` VARCHAR(24) NOT NULL COMMENT 'notification type',
  `title` VARCHAR(100) NOT NULL COMMENT 'title',
  `content` VARCHAR(500) NOT NULL COMMENT 'content',
  `is_read` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'read flag',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'deleted flag',
  `related_order_id` BIGINT DEFAULT NULL COMMENT 'related order id',
  `related_task_id` BIGINT DEFAULT NULL COMMENT 'related task id',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_notification_receiver_read_time` (`receiver_id`, `is_read`, `created_at`),
  KEY `idx_notification_receiver_deleted` (`receiver_id`, `is_deleted`),
  CONSTRAINT `fk_notification_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='notification table';

CREATE TABLE `review` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'review id',
  `order_id` BIGINT NOT NULL COMMENT 'order id',
  `reviewer_id` BIGINT NOT NULL COMMENT 'reviewer user id',
  `reviewee_id` BIGINT NOT NULL COMMENT 'reviewee user id',
  `rating` TINYINT NOT NULL COMMENT 'rating 1 to 5',
  `content` VARCHAR(500) DEFAULT NULL COMMENT 'review content',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_review_order_reviewer` (`order_id`, `reviewer_id`),
  KEY `idx_review_reviewee_id` (`reviewee_id`),
  CONSTRAINT `fk_review_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
  CONSTRAINT `fk_review_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_review_reviewee` FOREIGN KEY (`reviewee_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='review table';

CREATE TABLE `credit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'credit log id',
  `user_id` BIGINT NOT NULL COMMENT 'user id',
  `change_amount` INT NOT NULL COMMENT 'score delta',
  `score_before` INT NOT NULL COMMENT 'score before',
  `score_after` INT NOT NULL COMMENT 'score after',
  `reason` VARCHAR(100) NOT NULL COMMENT 'change reason',
  `related_order_id` BIGINT DEFAULT NULL COMMENT 'related order id',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_credit_log_user_time` (`user_id`, `created_at`),
  CONSTRAINT `fk_credit_log_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='credit log table';

CREATE TABLE `report` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'report id',
  `reporter_id` BIGINT NOT NULL COMMENT 'reporter user id',
  `target_type` VARCHAR(16) NOT NULL COMMENT 'report target type',
  `target_id` BIGINT NOT NULL COMMENT 'report target id',
  `reason_type` VARCHAR(16) NOT NULL COMMENT 'reason type',
  `description` VARCHAR(1000) NOT NULL COMMENT 'report description',
  `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'report status',
  `result` VARCHAR(500) DEFAULT NULL COMMENT 'processing result',
  `processed_by` BIGINT DEFAULT NULL COMMENT 'admin id',
  `processed_at` DATETIME DEFAULT NULL COMMENT 'processed time',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_report_reporter_status` (`reporter_id`, `status`),
  KEY `idx_report_admin_status_time` (`status`, `created_at`),
  CONSTRAINT `fk_report_reporter` FOREIGN KEY (`reporter_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_report_processor` FOREIGN KEY (`processed_by`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='report table';

CREATE TABLE `file_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'file record id',
  `user_id` BIGINT NOT NULL COMMENT 'uploader user id',
  `file_name` VARCHAR(256) NOT NULL COMMENT 'original file name',
  `file_url` VARCHAR(512) NOT NULL COMMENT 'file url',
  `file_size` BIGINT NOT NULL COMMENT 'file size in bytes',
  `purpose` VARCHAR(24) NOT NULL COMMENT 'file usage',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_file_record_user_id` (`user_id`),
  CONSTRAINT `fk_file_record_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='file record table';

CREATE TABLE `report_evidence` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'report evidence id',
  `report_id` BIGINT NOT NULL COMMENT 'report id',
  `file_record_id` BIGINT NOT NULL COMMENT 'evidence file record id',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_report_evidence_report_id` (`report_id`),
  KEY `idx_report_evidence_file_record_id` (`file_record_id`),
  CONSTRAINT `fk_report_evidence_report` FOREIGN KEY (`report_id`) REFERENCES `report` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_report_evidence_file_record` FOREIGN KEY (`file_record_id`) REFERENCES `file_record` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='report evidence relation table';

CREATE TABLE `announcement` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'announcement id',
  `publisher_id` BIGINT NOT NULL COMMENT 'publisher user id',
  `title` VARCHAR(100) NOT NULL COMMENT 'announcement title',
  `content` VARCHAR(5000) NOT NULL COMMENT 'announcement content',
  `priority` VARCHAR(10) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL or IMPORTANT',
  `is_active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'active flag',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_announcement_active_time` (`is_active`, `created_at`),
  CONSTRAINT `fk_announcement_publisher` FOREIGN KEY (`publisher_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='announcement table';

CREATE TABLE `admin_operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'admin operation log id',
  `admin_id` BIGINT NOT NULL COMMENT 'admin user id',
  `operation_type` VARCHAR(32) NOT NULL COMMENT 'operation type',
  `target_type` VARCHAR(16) NOT NULL COMMENT 'target type',
  `target_id` BIGINT NOT NULL COMMENT 'target id',
  `detail` VARCHAR(1000) DEFAULT NULL COMMENT 'operation detail',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_admin_operation_admin_time` (`admin_id`, `created_at`),
  CONSTRAINT `fk_admin_operation_admin` FOREIGN KEY (`admin_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='admin operation log table';

SET FOREIGN_KEY_CHECKS = 1;
