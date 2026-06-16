USE `campus_hub`;

SET NAMES utf8mb4;

UPDATE `user_profile`
SET `nickname` = '管理员演示',
    `college` = '信息化中心',
    `bio` = '系统管理员演示账号',
    `campus` = '仙林'
WHERE `id` = 1;

UPDATE `user_profile`
SET `nickname` = '学生演示一',
    `college` = '软件学院',
    `bio` = '已完成认证的学生演示账号',
    `campus` = '仙林',
    `contact` = '微信：student_demo1'
WHERE `id` = 2;

UPDATE `user_profile`
SET `nickname` = '学生演示二',
    `college` = '软件学院',
    `bio` = '另一个已认证的学生演示账号',
    `campus` = '仙林',
    `contact` = '微信：student_demo2'
WHERE `id` = 3;

UPDATE `user_profile`
SET `nickname` = '待认证学生',
    `college` = '软件学院',
    `bio` = '待完成邮箱认证的学生演示账号',
    `campus` = '仙林',
    `contact` = '电话：student_pending'
WHERE `id` = 4;

UPDATE `task`
SET `title` = '帮忙取快递',
    `description` = '需要帮忙从快递站取一个包裹并送到宿舍。',
    `campus` = '仙林',
    `location_detail` = '快递站到 9 栋宿舍',
    `category_fields` = JSON_OBJECT('pickupCode', 'ABCD1234', 'building', '9 栋宿舍')
WHERE `id` = 1;

UPDATE `task`
SET `title` = '出一本二手高数教材',
    `description` = '一本品相良好的二手高等数学教材，可线下交易。',
    `campus` = '鼓楼',
    `location_detail` = '教学楼 A 附近',
    `category_fields` = JSON_OBJECT('condition', '九成新', 'priceNote', '可小刀')
WHERE `id` = 2;

UPDATE `task`
SET `title` = '匿名咨询保研复试经验',
    `description` = '想咨询复试准备和申请流程相关经验。',
    `campus` = '仙林',
    `location_detail` = '线上交流',
    `category_fields` = JSON_OBJECT('topic', '保研复试')
WHERE `id` = 3;

UPDATE `task`
SET `title` = '打印并送资料',
    `description` = '需要有人帮忙打印材料并送到图书馆门口。',
    `campus` = '仙林',
    `location_detail` = '打印店到图书馆',
    `category_fields` = JSON_OBJECT('pages', 24)
WHERE `id` = 4;

UPDATE `application`
SET `message` = '我今天下午可以帮忙取。'
WHERE `id` = 1;

UPDATE `application`
SET `message` = '我今晚有空，可以接这个任务。'
WHERE `id` = 2;

UPDATE `order_status_log`
SET `reason` = '发布方确认接单申请'
WHERE `id` = 1;

UPDATE `order_message`
SET `content` = '打印好后请放在图书馆入口处。'
WHERE `id` = 1;

UPDATE `order_message`
SET `content` = '收到，我稍后会同步进度。'
WHERE `id` = 2;

UPDATE `notification`
SET `title` = '订单已开始进行',
    `content` = '你的跑腿订单已进入进行中状态。'
WHERE `id` = 1;

UPDATE `notification`
SET `title` = '收到新的接单申请',
    `content` = '需求“帮忙取快递”收到了一条新的接单申请。'
WHERE `id` = 2;

UPDATE `review`
SET `content` = '沟通顺畅，响应很快。'
WHERE `id` = 1;

UPDATE `credit_log`
SET `reason` = '订单完成奖励'
WHERE `id` = 1;

UPDATE `report`
SET `description` = '用于管理员流程测试的示例举报。',
    `result` = '已审核并作为测试记录关闭。'
WHERE `id` = 1;

UPDATE `announcement`
SET `title` = 'CampusHub 测试环境',
    `content` = '当前数据库包含本地开发用的示例数据，可用于注册、发布需求、接单、聊天、评价和举报流程测试。'
WHERE `id` = 1;

UPDATE `admin_operation_log`
SET `detail` = '处理了用于测试的示例举报'
WHERE `id` = 1;
