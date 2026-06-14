USE `campus_hub`;

SET NAMES utf8mb4;

INSERT INTO `user` (`id`, `email`, `password_hash`, `role`, `status`, `verified`, `student_no_masked`) VALUES
(1, 'admin.demo@smail.nju.edu.cn', '$2a$10$LuWuHEg376ubWf72H/GWSuBzr5YbXlIaJRDFITq0ERYX5/z9.ztLu', 'ADMIN', 'ACTIVE', 1, 'A0001'),
(2, 'student.demo1@smail.nju.edu.cn', '$2a$10$LuWuHEg376ubWf72H/GWSuBzr5YbXlIaJRDFITq0ERYX5/z9.ztLu', 'STUDENT', 'ACTIVE', 1, '2418****01'),
(3, 'student.demo2@smail.nju.edu.cn', '$2a$10$LuWuHEg376ubWf72H/GWSuBzr5YbXlIaJRDFITq0ERYX5/z9.ztLu', 'STUDENT', 'ACTIVE', 1, '2418****02'),
(4, 'student.pending@smail.nju.edu.cn', '$2a$10$LuWuHEg376ubWf72H/GWSuBzr5YbXlIaJRDFITq0ERYX5/z9.ztLu', 'STUDENT', 'ACTIVE', 0, '2418****03');

INSERT INTO `user_profile` (`id`, `user_id`, `nickname`, `avatar_url`, `gender`, `grade`, `college`, `bio`, `campus`, `contact`, `contact_visible`) VALUES
(1, 1, 'AdminDemo', NULL, 'OTHER', NULL, 'IT Center', 'System administrator account', 'Xianlin', NULL, 0),
(2, 2, 'StudentDemo1', NULL, 'MALE', '2024', 'Software School', 'Verified demo student account', 'Xianlin', 'wechat:student_demo1', 1),
(3, 3, 'StudentDemo2', NULL, 'FEMALE', '2024', 'Software School', 'Another verified demo student account', 'Xianlin', 'wechat:student_demo2', 1),
(4, 4, 'StudentPending', NULL, 'OTHER', '2025', 'Software School', 'Pending verification demo student account', 'Xianlin', 'phone:student_pending', 0);

INSERT INTO `verification_code` (`id`, `email`, `code`, `purpose`, `expires_at`, `used`) VALUES
(1, 'student.pending@smail.nju.edu.cn', '654321', 'REGISTER', DATE_ADD(NOW(), INTERVAL 1 DAY), 0),
(2, 'student.demo1@smail.nju.edu.cn', '123456', 'RESET_PASSWORD', DATE_ADD(NOW(), INTERVAL 1 DAY), 0);

INSERT INTO `task` (`id`, `publisher_id`, `category`, `title`, `description`, `campus`, `location_detail`, `reward_type`, `reward_amount`, `deadline`, `status`, `anonymous`, `category_fields`, `version`) VALUES
(1, 3, 'EXPRESS', 'Pick up a package', 'Need help picking up a package from the station and bringing it to the dorm.', 'Xianlin', 'Station to Dorm 9', 'CASH', 5.00, DATE_ADD(NOW(), INTERVAL 1 DAY), 'OPEN', 0, JSON_OBJECT('pickupCode', 'ABCD1234', 'building', 'Dorm9'), 0),
(2, 2, 'SECOND_HAND', 'Sell used calculus book', 'A second-hand calculus book in good condition, available for offline trade.', 'Gulou', 'Near Teaching Building A', 'CASH', 18.00, DATE_ADD(NOW(), INTERVAL 3 DAY), 'OPEN', 0, JSON_OBJECT('condition', '90% new', 'priceNote', 'negotiable'), 0),
(3, 3, 'CONSULTATION', 'Anonymous grad school advice', 'Need advice about interview preparation and application process.', 'Xianlin', 'Online chat', 'NEGOTIABLE', NULL, DATE_ADD(NOW(), INTERVAL 2 DAY), 'OPEN', 1, JSON_OBJECT('topic', 'grad_school'), 0),
(4, 2, 'ERRAND', 'Print and deliver documents', 'Need someone to print materials and deliver them to the library entrance.', 'Xianlin', 'Print shop to Library', 'CASH', 8.00, DATE_ADD(NOW(), INTERVAL 12 HOUR), 'IN_PROGRESS', 0, JSON_OBJECT('pages', 24), 1);

INSERT INTO `task_image` (`id`, `task_id`, `image_url`, `sort_order`) VALUES
(1, 2, '/uploads/tasks/book-1.jpg', 1),
(2, 4, '/uploads/tasks/print-order.jpg', 1);

INSERT INTO `favorite` (`id`, `user_id`, `task_id`) VALUES
(1, 2, 1),
(2, 3, 2);

INSERT INTO `application` (`id`, `task_id`, `applicant_id`, `message`, `status`) VALUES
(1, 1, 2, 'I can help pick it up this afternoon.', 'PENDING'),
(2, 4, 3, 'I am available tonight and can take this task.', 'APPROVED');

INSERT INTO `orders` (`id`, `task_id`, `publisher_id`, `service_provider_id`, `status`, `completion_proof_url`, `cancel_reason`, `version`) VALUES
(1, 4, 2, 3, 'IN_PROGRESS', NULL, NULL, 1);

INSERT INTO `order_status_log` (`id`, `order_id`, `from_status`, `to_status`, `operator_id`, `reason`) VALUES
(1, 1, 'PENDING_CONFIRM', 'IN_PROGRESS', 2, 'Publisher confirmed the application');

INSERT INTO `order_message` (`id`, `order_id`, `sender_id`, `message_type`, `content`, `image_url`, `is_read`) VALUES
(1, 1, 2, 'TEXT', 'Please leave it at the library entrance after printing.', NULL, 1),
(2, 1, 3, 'TEXT', 'Got it, I will send an update later.', NULL, 0);

INSERT INTO `notification` (`id`, `receiver_id`, `type`, `title`, `content`, `is_read`, `is_deleted`, `related_order_id`, `related_task_id`) VALUES
(1, 2, 'ORDER_STATUS', 'Order is in progress', 'Your errand order has entered the in-progress state.', 0, 0, 1, 4),
(2, 3, 'APPLICATION', 'New application received', 'Task "Pick up a package" received a new application.', 0, 0, NULL, 1);

INSERT INTO `review` (`id`, `order_id`, `reviewer_id`, `reviewee_id`, `rating`, `content`, `created_at`) VALUES
(1, 1, 2, 3, 5, 'Good communication and fast response.', DATE_SUB(NOW(), INTERVAL 1 DAY));

INSERT INTO `credit_log` (`id`, `user_id`, `change_amount`, `score_before`, `score_after`, `reason`, `related_order_id`) VALUES
(1, 3, 0, 100, 100, 'Order completion reward', 1);

INSERT INTO `report` (`id`, `reporter_id`, `target_type`, `target_id`, `reason_type`, `description`, `status`, `result`, `processed_by`, `processed_at`) VALUES
(1, 2, 'USER', 3, 'OTHER', 'Sample report for admin workflow testing.', 'RESOLVED', 'Reviewed and closed as a test record.', 1, NOW());

INSERT INTO `file_record` (`id`, `user_id`, `file_name`, `file_url`, `file_size`, `purpose`) VALUES
(1, 2, 'book-1.jpg', '/uploads/tasks/book-1.jpg', 182044, 'TASK_IMAGE'),
(2, 2, 'print-order.jpg', '/uploads/tasks/print-order.jpg', 92511, 'TASK_IMAGE'),
(3, 3, 'chat-proof.png', '/uploads/messages/chat-proof.png', 38220, 'CHAT_IMAGE');

INSERT INTO `announcement` (`id`, `publisher_id`, `title`, `content`, `priority`, `is_active`) VALUES
(1, 1, 'CampusHub test environment', 'This database contains local development seed data for registration, posting tasks, taking tasks, chat, review and report flows.', 'IMPORTANT', 1);

INSERT INTO `admin_operation_log` (`id`, `admin_id`, `operation_type`, `target_type`, `target_id`, `detail`) VALUES
(1, 1, 'HANDLE_REPORT', 'REPORT', 1, 'Processed sample report for testing');
