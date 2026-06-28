-- ============================================================
-- CampusHub 数据库建表脚本
-- 版本: MVP
-- 参考: docs/P3/04-er-diagram.md
-- ============================================================

CREATE DATABASE IF NOT EXISTS campus_hub
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE campus_hub;

-- ============================================================
-- 1. 用户认证与状态表 (User)
-- ============================================================
CREATE TABLE user (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '用户ID',
    email           VARCHAR(128)    NOT NULL                 COMMENT '学校邮箱',
    password_hash   VARCHAR(256)    NOT NULL                 COMMENT 'BCrypt 密码哈希',
    role            VARCHAR(16)     NOT NULL DEFAULT 'STUDENT' COMMENT '角色: STUDENT/ADMIN',
    status          VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/DISABLED/ANONYMIZED',
    verified        TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '是否已校园认证',
    login_failures  INT             NOT NULL DEFAULT 0       COMMENT '连续登录失败次数',
    locked_until    DATETIME        DEFAULT NULL             COMMENT '账号锁定截止时间',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_email (email),
    KEY idx_user_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户认证与状态表';

-- ============================================================
-- 2. 用户资料表 (UserProfile) — 1:1 关联 user
-- ============================================================
CREATE TABLE user_profile (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '资料ID',
    user_id         BIGINT          NOT NULL                 COMMENT '用户ID',
    nickname        VARCHAR(30)     NOT NULL                 COMMENT '昵称',
    avatar_url      VARCHAR(512)    DEFAULT NULL             COMMENT '头像URL',
    gender          VARCHAR(8)      DEFAULT NULL             COMMENT '性别: MALE/FEMALE/OTHER',
    grade           VARCHAR(16)     DEFAULT NULL             COMMENT '年级',
    college         VARCHAR(64)     DEFAULT NULL             COMMENT '学院',
    bio             VARCHAR(200)    DEFAULT NULL             COMMENT '个人简介',
    campus          VARCHAR(32)     DEFAULT NULL             COMMENT '常用校区',
    contact         VARCHAR(100)    DEFAULT NULL             COMMENT '联系方式',
    contact_visible TINYINT(1)      NOT NULL DEFAULT 1       COMMENT '联系方式是否公开',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_profile_user_id (user_id),
    CONSTRAINT fk_profile_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户资料表';

-- ============================================================
-- 3. 邮箱验证码表 (VerificationCode) — 扩展表
-- ============================================================
CREATE TABLE verification_code (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT 'ID',
    email           VARCHAR(128)    NOT NULL                 COMMENT '目标邮箱',
    code            VARCHAR(6)      NOT NULL                 COMMENT '6位验证码',
    purpose         VARCHAR(16)     NOT NULL                 COMMENT '用途: REGISTER/RESET_PASSWORD',
    expires_at      DATETIME        NOT NULL                 COMMENT '过期时间',
    used            TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '是否已使用',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_vc_email_code (email, code),
    KEY idx_vc_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邮箱验证码表';

-- ============================================================
-- 4. 需求/任务表 (Task)
-- ============================================================
CREATE TABLE task (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '任务ID',
    publisher_id    BIGINT          NOT NULL                 COMMENT '发布者ID',
    category        VARCHAR(32)     NOT NULL                 COMMENT '分类: EXPRESS/ERRAND/TUTORING/SECOND_HAND/LOST_FOUND/CONSULTATION/TEAM_UP/OTHER',
    title           VARCHAR(100)    NOT NULL                 COMMENT '标题',
    description     VARCHAR(2000)   NOT NULL                 COMMENT '描述',
    campus          VARCHAR(32)     NOT NULL                 COMMENT '校区/地点',
    location_detail VARCHAR(128)    DEFAULT NULL             COMMENT '详细地点',
    reward_type     VARCHAR(16)     NOT NULL                 COMMENT '结算类型: CASH(定价)/NEGOTIABLE(面议)/CREDIT_INTENT(积分)',
    reward_amount   DECIMAL(10,2)   DEFAULT NULL             COMMENT '定价金额，必须大于0',
    payment_method  VARCHAR(16)     DEFAULT NULL             COMMENT '结算方式: WECHAT/ALIPAY/CASH',
    deadline        DATETIME        NOT NULL                 COMMENT '截止时间',
    status          VARCHAR(20)     NOT NULL DEFAULT 'OPEN'  COMMENT '状态: OPEN/IN_PROGRESS/COMPLETED/CANCELLED/EXPIRED',
    anonymous       TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '是否匿名发布',
    category_fields JSON            DEFAULT NULL             COMMENT '分类差异化字段 (JSON)',
    version         INT             NOT NULL DEFAULT 0       COMMENT '乐观锁版本号',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_task_publisher (publisher_id),
    KEY idx_task_hall (status, category, campus),
    KEY idx_task_created_at (status, created_at),
    KEY idx_task_deadline (status, deadline),
    CONSTRAINT fk_task_publisher FOREIGN KEY (publisher_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='需求/任务表';

-- ============================================================
-- 5. 需求配图表 (TaskImage) — 1:N 关联 task
-- ============================================================
CREATE TABLE task_image (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '图片ID',
    task_id         BIGINT          NOT NULL                 COMMENT '任务ID',
    image_url       VARCHAR(512)    NOT NULL                 COMMENT '图片URL',
    sort_order      INT             NOT NULL DEFAULT 0       COMMENT '排序序号',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ti_task_id (task_id),
    CONSTRAINT fk_ti_task FOREIGN KEY (task_id) REFERENCES task(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='需求配图表';

-- ============================================================
-- 6. 收藏表 (Favorite) — 扩展表，M:N 关联 user-task
-- ============================================================
CREATE TABLE favorite (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT 'ID',
    user_id         BIGINT          NOT NULL                 COMMENT '用户ID',
    task_id         BIGINT          NOT NULL                 COMMENT '任务ID',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_favorite (user_id, task_id),
    KEY idx_fav_user (user_id),
    KEY idx_fav_task (task_id),
    CONSTRAINT fk_fav_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    CONSTRAINT fk_fav_task FOREIGN KEY (task_id) REFERENCES task(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户收藏表';

-- ============================================================
-- 7. 接单申请表 (Application)
-- ============================================================
CREATE TABLE application (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '申请ID',
    task_id         BIGINT          NOT NULL                 COMMENT '任务ID',
    applicant_id    BIGINT          NOT NULL                 COMMENT '申请人ID',
    message         VARCHAR(500)    NOT NULL                 COMMENT '申请留言',
    status          VARCHAR(16)     NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/APPROVED/REJECTED/CANCELLED',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_app_task (task_id, status),
    KEY idx_app_applicant (applicant_id),
    CONSTRAINT fk_app_task FOREIGN KEY (task_id) REFERENCES task(id),
    CONSTRAINT fk_app_applicant FOREIGN KEY (applicant_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='接单申请表';

-- ============================================================
-- 8. 订单表 (Order) — 注意表名用 orders 避免保留字
-- ============================================================
CREATE TABLE orders (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '订单ID',
    task_id             BIGINT          NOT NULL                 COMMENT '关联任务ID',
    publisher_id        BIGINT          NOT NULL                 COMMENT '发布者（需求方）ID',
    service_provider_id BIGINT          DEFAULT NULL             COMMENT '服务方ID，订单退回待确认时可为空',
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING_CONFIRM' COMMENT '状态: PENDING_CONFIRM/IN_PROGRESS/PENDING_COMPLETION/COMPLETED/CANCELLED/TIMEOUT/DISPUTE/REVIEWED',
    completion_proof_url VARCHAR(512)   DEFAULT NULL             COMMENT '完成凭证图片URL',
    cancel_reason       VARCHAR(500)    DEFAULT NULL             COMMENT '取消原因',
    version             INT             NOT NULL DEFAULT 0       COMMENT '乐观锁版本号',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_task (task_id),
    KEY idx_order_publisher (publisher_id, status),
    KEY idx_order_provider (service_provider_id, status),
    CONSTRAINT fk_order_task FOREIGN KEY (task_id) REFERENCES task(id),
    CONSTRAINT fk_order_publisher FOREIGN KEY (publisher_id) REFERENCES user(id),
    CONSTRAINT fk_order_provider FOREIGN KEY (service_provider_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- ============================================================
-- 9. 订单状态流转日志表 (OrderStatusLog)
-- ============================================================
CREATE TABLE order_status_log (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '日志ID',
    order_id        BIGINT          NOT NULL                 COMMENT '订单ID',
    from_status     VARCHAR(20)     NOT NULL                 COMMENT '变更前状态',
    to_status       VARCHAR(20)     NOT NULL                 COMMENT '变更后状态',
    operator_id     BIGINT          NOT NULL                 COMMENT '操作人ID',
    reason          VARCHAR(500)    DEFAULT NULL             COMMENT '变更原因',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_osl_order (order_id, created_at),
    CONSTRAINT fk_osl_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_osl_operator FOREIGN KEY (operator_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单状态流转日志表';

-- ============================================================
-- 10. 订单聊天消息表 (OrderMessage)
-- ============================================================
CREATE TABLE order_message (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '消息ID',
    order_id        BIGINT          NOT NULL                 COMMENT '订单ID',
    sender_id       BIGINT          NOT NULL                 COMMENT '发送者ID',
    message_type    VARCHAR(8)      NOT NULL                 COMMENT '消息类型: TEXT/IMAGE',
    content         VARCHAR(2000)   DEFAULT NULL             COMMENT '文字内容',
    image_url       VARCHAR(512)    DEFAULT NULL             COMMENT '图片URL',
    is_read         TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '是否已读',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_om_order_time (order_id, created_at),
    CONSTRAINT fk_om_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_om_sender FOREIGN KEY (sender_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单聊天消息表';

-- ============================================================
-- 11. 系统通知表 (Notification)
-- ============================================================
CREATE TABLE notification (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '通知ID',
    receiver_id     BIGINT          NOT NULL                 COMMENT '接收者ID',
    type            VARCHAR(20)     NOT NULL                 COMMENT '通知类型: APPLICATION/ORDER_STATUS/REVIEW_REQUEST/REPORT_RESULT',
    title           VARCHAR(100)    NOT NULL                 COMMENT '通知标题',
    content         VARCHAR(500)    NOT NULL                 COMMENT '通知内容',
    is_read         TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '是否已读',
    is_deleted      TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '是否已删除（软删除）',
    related_order_id BIGINT         DEFAULT NULL             COMMENT '关联订单ID',
    related_task_id  BIGINT         DEFAULT NULL             COMMENT '关联任务ID',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_notif_receiver (receiver_id, is_read, created_at),
    KEY idx_notif_deleted (receiver_id, is_deleted),
    CONSTRAINT fk_notif_receiver FOREIGN KEY (receiver_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统通知表';

-- ============================================================
-- 12. 评价表 (Review)
-- ============================================================
CREATE TABLE review (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '评价ID',
    order_id        BIGINT          NOT NULL                 COMMENT '订单ID',
    reviewer_id     BIGINT          NOT NULL                 COMMENT '评价者ID',
    reviewee_id     BIGINT          NOT NULL                 COMMENT '被评价者ID',
    rating          TINYINT         NOT NULL                 COMMENT '评分 1-5',
    content         VARCHAR(500)    DEFAULT NULL             COMMENT '评价内容',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_review (order_id, reviewer_id),
    KEY idx_review_reviewee (reviewee_id),
    CONSTRAINT fk_review_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_review_reviewer FOREIGN KEY (reviewer_id) REFERENCES user(id),
    CONSTRAINT fk_review_reviewee FOREIGN KEY (reviewee_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价表';

-- ============================================================
-- 13. 信用分变动流水表 (CreditLog) — 扩展表
-- ============================================================
CREATE TABLE credit_log (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '日志ID',
    user_id         BIGINT          NOT NULL                 COMMENT '用户ID',
    change_amount   INT             NOT NULL                 COMMENT '变动分值 (+/-)',
    score_before    INT             NOT NULL                 COMMENT '变动前分数',
    score_after     INT             NOT NULL                 COMMENT '变动后分数',
    reason          VARCHAR(100)    NOT NULL                 COMMENT '变动原因',
    related_order_id BIGINT         DEFAULT NULL             COMMENT '关联订单ID',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_cl_user (user_id, created_at),
    CONSTRAINT fk_cl_user FOREIGN KEY (user_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='信用分变动流水表';

-- ============================================================
-- 14. 举报表 (Report)
-- ============================================================
CREATE TABLE report (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '举报ID',
    reporter_id     BIGINT          NOT NULL                 COMMENT '举报人ID',
    target_type     VARCHAR(16)     NOT NULL                 COMMENT '举报对象类型: TASK/ORDER_MESSAGE/REVIEW/USER',
    target_id       BIGINT          NOT NULL                 COMMENT '被举报对象ID',
    related_order_id BIGINT         DEFAULT NULL             COMMENT '关联订单ID',
    reason_type     VARCHAR(16)     NOT NULL                 COMMENT '举报类型: FRAUD/ABUSE/SPAM/ILLEGAL/TIMEOUT/OTHER',
    description     VARCHAR(1000)   NOT NULL                 COMMENT '举报描述',
    status          VARCHAR(16)     NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/PROCESSING/RESOLVED/REJECTED',
    result          VARCHAR(500)    DEFAULT NULL             COMMENT '处理结果说明',
    processed_by    BIGINT          DEFAULT NULL             COMMENT '处理人（管理员）ID',
    processed_at    DATETIME        DEFAULT NULL             COMMENT '处理时间',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_report_reporter (reporter_id, status),
    KEY idx_report_admin (status, created_at),
    KEY idx_report_related_order (related_order_id, reason_type, status),
    CONSTRAINT fk_report_reporter FOREIGN KEY (reporter_id) REFERENCES user(id),
    CONSTRAINT fk_report_related_order FOREIGN KEY (related_order_id) REFERENCES orders(id),
    CONSTRAINT fk_report_processor FOREIGN KEY (processed_by) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='举报表';

-- ============================================================
-- 15. 文件上传记录表 (FileRecord) — 扩展表
-- ============================================================
CREATE TABLE file_record (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '文件ID',
    user_id         BIGINT          NOT NULL                 COMMENT '上传者ID',
    file_name       VARCHAR(256)    NOT NULL                 COMMENT '原始文件名',
    file_url        VARCHAR(512)    NOT NULL                 COMMENT '文件访问URL',
    file_size       BIGINT          NOT NULL                 COMMENT '文件大小 (bytes)',
    purpose         VARCHAR(20)     NOT NULL                 COMMENT '用途: AVATAR/TASK_IMAGE/ORDER_PROOF/CHAT_IMAGE/REPORT_EVIDENCE',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_fr_user (user_id),
    CONSTRAINT fk_fr_user FOREIGN KEY (user_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件上传记录表';

-- ============================================================
-- 16. 公告表 (Announcement) — 扩展表
-- ============================================================
CREATE TABLE announcement (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '公告ID',
    publisher_id    BIGINT          NOT NULL                 COMMENT '发布者（管理员）ID',
    title           VARCHAR(100)    NOT NULL                 COMMENT '标题',
    content         VARCHAR(5000)   NOT NULL                 COMMENT '内容',
    priority        VARCHAR(10)     NOT NULL DEFAULT 'NORMAL' COMMENT '优先级: NORMAL/IMPORTANT',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ann_time (created_at),
    CONSTRAINT fk_ann_publisher FOREIGN KEY (publisher_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告表';

-- ============================================================
-- 17. 管理员操作审计日志表 (AdminOperationLog) — 扩展表
-- ============================================================
CREATE TABLE admin_operation_log (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '日志ID',
    admin_id        BIGINT          NOT NULL                 COMMENT '管理员ID',
    operation_type  VARCHAR(32)     NOT NULL                 COMMENT '操作类型: DISABLE_USER/ENABLE_USER/CANCEL_TASK/FREEZE_ORDER/HANDLE_REPORT/PUBLISH_ANNOUNCEMENT',
    target_type     VARCHAR(16)     NOT NULL                 COMMENT '操作目标类型: USER/TASK/ORDER/REPORT/ANNOUNCEMENT',
    target_id       BIGINT          NOT NULL                 COMMENT '操作目标ID',
    detail          VARCHAR(1000)   DEFAULT NULL             COMMENT '操作详情',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_aol_admin_time (admin_id, created_at),
    CONSTRAINT fk_aol_admin FOREIGN KEY (admin_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员操作审计日志表';

-- ============================================================
-- 初始化数据：管理员账号
-- 密码: Admin123! (BCrypt 哈希，生产环境需替换)
-- ============================================================
INSERT INTO user (email, password_hash, role, status, verified) VALUES
('admin@smail.nju.edu.cn', '$2a$10$placeholder_admin_bcrypt_hash', 'ADMIN', 'ACTIVE', 1);

INSERT INTO user_profile (user_id, nickname, campus) VALUES
(1, '系统管理员', '仙林校区');
