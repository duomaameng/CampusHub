# P4 任务 3：API 接口任务清单

本文档基于 P3 的接口设计稿整理，用于 P4 联调阶段跟踪接口实现与验证情况。

说明：
- 本文档以**当前后端实际路径口径**为准，统一已实现接口的路径与方法。
- 对于文档中已经规划、但当前尚未实现的接口，**保留条目，不删除**。
- 若联调发现 Bug，请在 [08-bug-fix-log.md](C:\Users\duoma\java\软工2项目\CampusHub\docs\P4\04-bug-fix-log.md) 中记录。

---

## 1. 认证模块（Auth）

| 序号 | 方法 | 路径 | 说明 | 认证 | 状态 |
|------|------|------|------|------|------|
| A-01 | POST | `/api/auth/register` | 用户注册 | 无 | 已完成 |
| A-02 | POST | `/api/auth/login` | 登录，返回 JWT | 无 | 已完成 |
| A-03 | POST | `/api/auth/logout` | 退出登录 | JWT | 已完成 |
| A-04 | POST | `/api/auth/send-verification-code` | 发送邮箱验证码 | 无 | 已完成 |
| A-05 | POST | `/api/auth/verify-email` | 校验邮箱验证码 | 无 | 已完成 |
| A-06 | POST | `/api/auth/reset-password` | 验证码重置密码 | 无 | 已完成 |

---

## 2. 用户模块（User）

| 序号 | 方法 | 路径 | 说明 | 认证 | 状态  |
|------|------|------|------|------|-----|
| U-01 | GET | `/api/users/me` | 获取当前用户资料 | JWT | 已完成 |
| U-02 | PATCH | `/api/users/me` | 修改个人资料 | JWT | 已完成 |
| U-03 | DELETE | `/api/users/me` | 注销账号 | JWT | 已完成 |
| U-04 | GET | `/api/users/{userId}/profile` | 获取用户公开资料 | JWT | 已完成 |
| U-05 | GET | `/api/users/{userId}/reviews` | 获取用户评价记录 | JWT | 已完成 |
| U-06 | GET | `/api/users/{userId}/credit` | 获取用户信用信息 | JWT | 已完成 |

---

## 3. 需求与任务大厅（Task）

| 序号 | 方法 | 路径 | 说明 | 认证 | 状态  |
|------|------|------|------|------|-----|
| T-01 | GET | `/api/tasks` | 任务大厅列表（分类 / 校区 / 关键词筛选 + 排序 + 分页） | 无 | 已完成 |
| T-02 | POST | `/api/tasks` | 发布需求 | JWT | 已完成 |
| T-03 | GET | `/api/tasks/{taskId}` | 需求详情 | 无 | 已完成 |
| T-04 | PATCH | `/api/tasks/{taskId}` | 编辑需求（仅发布者，未接单时可操作） | JWT | 已完成 |
| T-05 | DELETE | `/api/tasks/{taskId}` | 删除需求（仅发布者，未接单时可操作） | JWT | 已完成 |
| T-06 | POST | `/api/tasks/{taskId}/favorite` | 收藏 / 取消收藏 | JWT | 已完成 |
| T-07 | GET | `/api/tasks/favorites` | 我的收藏列表 | JWT | 已完成 |

---

## 4. 接单申请（Application）

| 序号 | 方法 | 路径 | 说明 | 认证 | 状态  |
|------|------|------|------|------|-----|
| AP-01 | GET | `/api/tasks/{taskId}/applications` | 查看需求的所有接单申请 | JWT | 已完成 |
| AP-02 | POST | `/api/tasks/{taskId}/applications` | 发起接单申请 | JWT | 已完成 |
| AP-03 | POST | `/api/applications/{applicationId}/confirm` | 确认接单（生成订单） | JWT | 已完成 |
| AP-04 | POST | `/api/applications/{applicationId}/reject` | 拒绝接单申请 | JWT | 已完成 |

---

## 5. 订单模块（Order）

| 序号   | 方法 | 路径 | 说明 | 认证 | 状态  |
|------|------|------|------|------|-----|
| O-01 | GET | `/api/orders` | 我的订单列表（按角色 / 状态筛选 + 关键词） | JWT | 已完成 |
| O-02 | GET | `/api/orders/{orderId}` | 订单详情（含状态日志和聊天记录） | JWT | 已完成 |
| O-03 | POST | `/api/orders/{orderId}/complete` | 服务方提交完成凭证 | JWT | 已完成 |
| O-04 | POST | `/api/orders/{orderId}/confirm-completion` | 发布者确认完成 | JWT | 已完成 |
| O-05 | POST | `/api/orders/{orderId}/cancel` | 取消订单 | JWT | 已完成 |
| O-06 | GET | `/api/orders/{orderId}/status-logs` | 订单状态变更日志 | JWT | 已完成 |

---

## 6. 订单聊天消息（Message）

| 序号 | 方法 | 路径 | 说明                                | 认证 | 状态  |
|------|------|------|-----------------------------------|------|-----|
| M-01 | GET | `/api/orders/{orderId}/messages` | 获取订单聊天记录                          | JWT | 已完成 |
| M-02 | POST | `/api/orders/{orderId}/messages` | 发送文字消息                            | JWT | 已完成 |
| M-03 | POST | `/api/orders/{orderId}/messages` | 发送图片消息（通过 `messageType=image` 区分） | JWT | 已完成 |

---

## 7. 评价与信用（Review）

| 序号 | 方法 | 路径 | 说明 | 认证 | 状态  |
|------|------|------|------|------|-----|
| R-01 | GET | `/api/orders/{orderId}/reviews` | 查看订单评价 | JWT | 已完成 |
| R-02 | POST | `/api/orders/{orderId}/reviews` | 提交评价 | JWT | 已完成 |

---

## 8. 通知模块（Notification）

| 序号 | 方法 | 路径 | 说明 | 认证 | 状态  |
|------|------|------|------|------|-----|
| N-01 | GET | `/api/notifications` | 通知列表（按类型 / 已读筛选 + 分页） | JWT | 已完成 |
| N-02 | GET | `/api/notifications/unread-count` | 未读通知数量 | JWT | 已完成 |
| N-03 | PATCH | `/api/notifications/read-all` | 全部标记为已读 | JWT | 已完成 |
| N-04 | PATCH | `/api/notifications/{notificationId}/read` | 标记通知为已读 | JWT | 已完成 |
| N-05 | DELETE | `/api/notifications/{notificationId}` | 删除通知 | JWT | 已完成 |

---

## 9. 举报模块（Report）

| 序号 | 方法 | 路径 | 说明 | 认证 | 状态  |
|------|------|------|------|------|-----|
| RP-01 | GET | `/api/reports` | 我的举报列表 | JWT | 已完成 |
| RP-02 | POST | `/api/tasks/{taskId}/reports` | 提交任务举报 | JWT | 已完成 |
| RP-03 | POST | `/api/users/{userId}/reports` | 提交用户举报 | JWT | 已完成 |
| RP-04 | GET | `/api/reports/{reportId}` | 举报详情 | JWT | 已完成 |

---

## 10. 文件上传（File）

| 序号 | 方法 | 路径 | 说明 | 认证 | 状态  |
|------|------|------|------|------|-----|
| F-01 | POST | `/api/files/upload` | 上传图片（头像 / 任务配图 / 完成凭证 / 聊天图片 / 举报证据） | JWT | 已完成 |

---

## 11. 后台管理（Admin）

| 序号 | 方法 | 路径 | 说明 | 认证 | 状态  |
|------|------|------|------|------|-----|
| AD-01 | GET | `/api/admin/dashboard` | 后台概览数据 | ADMIN | 已完成 |
| AD-02 | GET | `/api/admin/users` | 用户管理列表 | ADMIN | 已完成 |
| AD-03 | GET | `/api/admin/users/{userId}` | 用户详情（管理员视角） | ADMIN | 已完成 |
| AD-04 | PATCH | `/api/admin/users/{userId}/status` | 禁用 / 解禁用户 | ADMIN | 已完成 |
| AD-05 | GET | `/api/admin/tasks` | 需求管理列表 | ADMIN | 已完成 |
| AD-06 | PATCH | `/api/admin/tasks/{taskId}/status` | 下架需求 | ADMIN | 已完成 |
| AD-07 | GET | `/api/admin/orders` | 订单管理列表 | ADMIN | 已完成 |
| AD-08 | PATCH | `/api/admin/orders/{orderId}/status` | 冻结 / 恢复订单 | ADMIN | 已完成 |
| AD-09 | GET | `/api/admin/reports` | 举报管理列表 | ADMIN | 已完成 |
| AD-10 | PATCH | `/api/admin/reports/{reportId}` | 处理举报 | ADMIN | 已完成 |

---

## 12. 公告模块（Announcement）

| 序号 | 方法 | 路径 | 说明 | 认证 | 状态  |
|------|------|------|------|------|-----|
| AN-01 | GET | `/api/announcements` | 前台公告列表 | 无 | 已完成 |
| AN-02 | GET | `/api/admin/announcements` | 公告列表（管理端） | ADMIN | 已完成 |
| AN-03 | POST | `/api/admin/announcements` | 发布公告 | ADMIN | 已完成 |
| AN-04 | PATCH | `/api/admin/announcements/{announcementId}` | 编辑公告 | ADMIN | 已完成 |
| AN-05 | DELETE | `/api/admin/announcements/{announcementId}` | 删除公告 | ADMIN | 已完成 |

---

## 汇总

| 模块 | 接口数    |
|------|--------|
| Auth 认证 | 6      |
| User 用户 | 6      |
| Task 需求 | 7      |
| Application 接单 | 4      |
| Order 订单 | 6      |
| Message 聊天 | 3      |
| Review 评价 | 2      |
| Notification 通知 | 5      |
| Report 举报 | 4      |
| File 文件 | 1      |
| Admin 后台 | 10     |
| Announcement 公告 | 5      |
| **合计** | **59** |

---

## 使用说明

1. 测试通过后，将对应行的“状态”列改为“已完成”。
2. 发现 Bug 时将状态改为“有 Bug”，并在 [08-bug-fix-log.md](C:\Users\duoma\java\软工2项目\CampusHub\docs\P4\04-bug-fix-log.md) 追加记录。
3. 所有接口联调完成后，再进行主链路收口与验收。
