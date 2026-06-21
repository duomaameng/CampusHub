USE `campus_hub`;

SET NAMES utf8mb4;

INSERT INTO `user` (`id`, `email`, `password_hash`, `role`, `status`, `verified`, `student_no_masked`) VALUES
(1, 'admin.demo@smail.nju.edu.cn', '$2a$10$LuWuHEg376ubWf72H/GWSuBzr5YbXlIaJRDFITq0ERYX5/z9.ztLu', 'ADMIN', 'ACTIVE', 1, 'A0001'),
(2, 'student.demo1@smail.nju.edu.cn', '$2a$10$LuWuHEg376ubWf72H/GWSuBzr5YbXlIaJRDFITq0ERYX5/z9.ztLu', 'STUDENT', 'ACTIVE', 1, '2418****01'),
(3, 'student.demo2@smail.nju.edu.cn', '$2a$10$LuWuHEg376ubWf72H/GWSuBzr5YbXlIaJRDFITq0ERYX5/z9.ztLu', 'STUDENT', 'ACTIVE', 1, '2418****02'),
(4, 'student.pending@smail.nju.edu.cn', '$2a$10$LuWuHEg376ubWf72H/GWSuBzr5YbXlIaJRDFITq0ERYX5/z9.ztLu', 'STUDENT', 'ACTIVE', 0, '2418****03');

INSERT INTO `user_profile` (`id`, `user_id`, `nickname`, `avatar_url`, `gender`, `grade`, `college`, `bio`, `campus`, `contact`, `contact_visible`) VALUES
(1, 1, '管理员', NULL, 'OTHER', NULL, '信息化中心', '系统管理员演示账号', '仙林', NULL, 0),
(2, 2, 'duomaameng', NULL, 'MALE', '2024', '软件学院', '已完成认证的学生演示账号', '仙林', '微信：student_demo1', 1),
(3, 3, 'Kurisu188', NULL, 'FEMALE', '2024', '软件学院', '另一个已认证的学生演示账号', '仙林', '微信：student_demo2', 1),
(4, 4, 'petrichor', NULL, 'OTHER', '2025', '软件学院', '待完成邮箱认证的学生演示账号', '仙林', '电话：student_pending', 0);

INSERT INTO `verification_code` (`id`, `email`, `code`, `purpose`, `expires_at`, `used`) VALUES
(1, 'student.pending@smail.nju.edu.cn', '654321', 'REGISTER', DATE_ADD(NOW(), INTERVAL 1 DAY), 0),
(2, 'student.demo1@smail.nju.edu.cn', '123456', 'RESET_PASSWORD', DATE_ADD(NOW(), INTERVAL 1 DAY), 0);

INSERT INTO `task` (`id`, `publisher_id`, `category`, `title`, `description`, `campus`, `location_detail`, `reward_type`, `reward_amount`, `deadline`, `status`, `anonymous`, `category_fields`, `version`) VALUES
(1, 3, 'EXPRESS', '帮忙取快递', '需要帮忙从快递站取一个包裹并送到宿舍。', '仙林', '快递站到 9 栋宿舍', 'CASH', 5.00, '2026-08-08 18:00:00', 'OPEN', 0, JSON_OBJECT('pickupCode', '46-1-7995', 'building', '9 栋宿舍'), 0),
(2, 2, 'SECOND_HAND', '出一本二手高数教材', '一本品相良好的二手高等数学教材，可线下交易。', '鼓楼', '教学楼 A 附近', 'CASH', 18.00, '2026-08-12 20:00:00', 'OPEN', 0, JSON_OBJECT('condition', '九成新', 'priceNote', '可小刀'), 0),
(3, 3, 'CONSULTATION', '匿名咨询保研复试经验', '想咨询复试准备和申请流程相关经验。', '仙林', '线上交流', 'NEGOTIABLE', NULL, '2026-08-10 20:00:00', 'OPEN', 1, JSON_OBJECT('topic', '保研复试'), 0),
(4, 2, 'ERRAND', '打印并送资料', '需要有人帮忙打印材料并送到图书馆门口。', '仙林', '打印店到图书馆', 'CASH', 8.00, '2026-08-05 18:00:00', 'IN_PROGRESS', 0, JSON_OBJECT('pages', 24), 1);

INSERT INTO `task_image` (`id`, `task_id`, `image_url`, `sort_order`) VALUES
(1, 2, '/uploads/image_editor_1781931903418..jpg', 1);

INSERT INTO `favorite` (`id`, `user_id`, `task_id`) VALUES
(1, 2, 1),
(2, 3, 2);

INSERT INTO `application` (`id`, `task_id`, `applicant_id`, `message`, `status`) VALUES
(1, 1, 2, '我今天下午可以帮忙取。', 'PENDING'),
(2, 4, 3, '我今晚有空，可以接这个任务。', 'APPROVED');

INSERT INTO `orders` (`id`, `task_id`, `publisher_id`, `service_provider_id`, `status`, `completion_proof_url`, `cancel_reason`, `version`) VALUES
(1, 4, 2, 3, 'IN_PROGRESS', NULL, NULL, 1);

INSERT INTO `order_status_log` (`id`, `order_id`, `from_status`, `to_status`, `operator_id`, `reason`) VALUES
(1, 1, 'PENDING_CONFIRM', 'IN_PROGRESS', 2, '发布方确认接单申请');

INSERT INTO `order_message` (`id`, `order_id`, `sender_id`, `message_type`, `content`, `image_url`, `is_read`) VALUES
(1, 1, 2, 'TEXT', '打印好后请放在图书馆入口处。', NULL, 1),
(2, 1, 3, 'TEXT', '收到，我稍后会同步进度。', NULL, 0);

INSERT INTO `notification` (`id`, `receiver_id`, `type`, `title`, `content`, `is_read`, `is_deleted`, `related_order_id`, `related_task_id`) VALUES
(1, 2, 'ORDER_STATUS', '订单已开始进行', '你的跑腿订单已进入进行中状态。', 0, 0, 1, 4),
(2, 3, 'APPLICATION', '收到新的接单申请', '需求“帮忙取快递”收到了一条新的接单申请。', 0, 0, NULL, 1);

INSERT INTO `credit_log` (`id`, `user_id`, `change_amount`, `score_before`, `score_after`, `reason`, `related_order_id`) VALUES
(1, 3, 0, 100, 100, '订单完成奖励', 1);

INSERT INTO `report` (`id`, `reporter_id`, `target_type`, `target_id`, `reason_type`, `description`, `status`, `result`, `processed_by`, `processed_at`) VALUES
(1, 2, 'USER', 3, 'OTHER', '用于管理员流程测试的示例举报。', 'RESOLVED', '已审核并作为测试记录关闭。', 1, NOW());

INSERT INTO `file_record` (`id`, `user_id`, `file_name`, `file_url`, `file_size`, `purpose`) VALUES
(1, 2, 'book-1.jpg', '/uploads/tasks/book-1.jpg', 182044, 'TASK_IMAGE'),
(2, 2, 'print-order.jpg', '/uploads/tasks/print-order.jpg', 92511, 'TASK_IMAGE'),
(3, 3, 'chat-proof.png', '/uploads/messages/chat-proof.png', 38220, 'CHAT_IMAGE');

INSERT INTO `announcement` (`id`, `publisher_id`, `title`, `content`, `priority`, `is_active`) VALUES
(1, 1, 'CampusHub 测试环境', '当前数据库包含本地开发用的示例数据，可用于注册、发布需求、接单、聊天、评价和举报流程测试。', 'IMPORTANT', 1);

INSERT INTO `admin_operation_log` (`id`, `admin_id`, `operation_type`, `target_type`, `target_id`, `detail`) VALUES
(1, 1, 'HANDLE_REPORT', 'REPORT', 1, '处理了用于测试的示例举报');
