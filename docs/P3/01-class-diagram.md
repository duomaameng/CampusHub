# CampusHub 类图

**阶段：** P3 详细设计  
**范围：** 核心业务类图（MVP 首版）

## 1. 设计范围说明

本类图围绕 CampusHub 的核心业务闭环展开：

`用户注册登录 -> 发布需求 -> 接单申请 -> 形成订单 -> 订单内交流 -> 完成订单 -> 提交评价 -> 发起举报`

本图优先覆盖 P1/P2 已确定的核心模块，不在第一版类图中展开验证码、收藏、公告、申诉、处罚、黑名单等扩展对象，以保证结构清晰、重点突出。

## 2. 核心类图

```mermaid
classDiagram
direction LR

class User {
  +Long id
  +String email
  +String passwordHash
  +UserRole role
  +UserStatus status
  +Boolean verified
  +canLogin() boolean
  +isAdmin() boolean
  +markVerified() void
  +disable() void
}

class UserProfile {
  +Long userId
  +String nickname
  +String avatarUrl
  +String campus
  +String contact
  +Boolean contactVisible
  +updateProfile() void
  +hideContact() void
  +showContact() void
}

class Task {
  +Long id
  +Long publisherId
  +TaskCategory category
  +String title
  +String description
  +String campus
  +LocalDateTime deadline
  +TaskStatus status
  +Boolean anonymous
  +publish() void
  +edit() void
  +cancel() void
  +expire() void
  +canAcceptApplication() boolean
}

class TaskImage {
  +Long id
  +Long taskId
  +String imageUrl
  +Integer sortOrder
  +bindToTask(taskId) void
}

class Application {
  +Long id
  +Long taskId
  +Long applicantId
  +String message
  +ApplicationStatus status
  +LocalDateTime createdAt
  +submit() void
  +approve() void
  +reject() void
  +cancel() void
}

class Order {
  +Long id
  +Long taskId
  +Long publisherId
  +Long serviceProviderId
  +OrderStatus status
  +String completionProofUrl
  +LocalDateTime createdAt
  +confirm() void
  +submitCompletion(proofUrl) void
  +confirmCompletion() void
  +cancel(reason) void
  +enterDispute() void
  +canBeReviewed() boolean
}

class OrderStatusLog {
  +Long id
  +Long orderId
  +OrderStatus fromStatus
  +OrderStatus toStatus
  +Long operatorId
  +String reason
  +LocalDateTime createdAt
  +recordTransition() void
}

class OrderMessage {
  +Long id
  +Long orderId
  +Long senderId
  +MessageType messageType
  +String content
  +String imageUrl
  +Boolean read
  +LocalDateTime createdAt
  +sendText(content) void
  +sendImage(imageUrl) void
  +markRead() void
}

class Notification {
  +Long id
  +Long receiverId
  +NotificationType type
  +String title
  +String content
  +Boolean read
  +LocalDateTime createdAt
  +markRead() void
  +markDeleted() void
}

class Review {
  +Long id
  +Long orderId
  +Long reviewerId
  +Long revieweeId
  +Integer rating
  +String content
  +LocalDateTime createdAt
  +submit() void
  +isValidRating() boolean
}

class Report {
  +Long id
  +Long reporterId
  +ReportTargetType targetType
  +Long targetId
  +ReportReasonType reasonType
  +String description
  +ReportStatus status
  +LocalDateTime createdAt
  +submit() void
  +process() void
  +reject() void
}

User "1" -- "1" UserProfile : has
User "1" -- "0..*" Task : publishes
Task "1" *-- "0..*" TaskImage : contains
Task "1" -- "0..*" Application : receives
User "1" -- "0..*" Application : submits
Application "0..*" --> "1" Task : applies for
Application "0..*" --> "1" User : applicant
Task "1" -- "0..1" Order : generates
Order "1" *-- "1..*" OrderStatusLog : records
Order "1" *-- "0..*" OrderMessage : contains
Order "1" -- "0..*" Review : receives
User "1" -- "0..*" Review : writes
User "1" -- "0..*" Notification : receives
Order "1" ..> Notification : triggers
User "1" -- "0..*" Report : submits
Report "0..*" --> "1" User : reporter
Order "0..1" --> "1" Task : created from
Order "0..*" --> "1" User : publisher
Order "0..*" --> "1" User : service provider
OrderMessage "0..*" --> "1" User : sender
Notification "0..*" --> "1" User : receiver
Review "0..*" --> "1" User : reviewer
Review "0..*" --> "1" User : reviewee
Report ..> Task : target
Report ..> OrderMessage : target
Report ..> Review : target
Report ..> User : target
```

## 3. 关系说明

- `User` 是平台参与者核心，负责发布需求、申请接单、发送消息、提交评价与举报。
- `Task` 是需求发布对象，`Application` 表示接单申请，申请被确认后生成 `Order`。
- `Order` 是执行主线中心对象，状态变化写入 `OrderStatusLog`，沟通消息写入 `OrderMessage`。
- `Notification` 与 `OrderMessage` 不同：前者是系统通知，后者是订单内聊天消息。
- `Review` 表示订单完成后的双向评价，`Report` 表示对任务、消息、评价或用户行为的举报。

## 4. 第一版取舍说明

本版类图优先突出主业务骨架，因此未纳入以下扩展类：

- `VerificationCode`
- `Favorite`
- `CreditLog`
- `Appeal`
- `Punishment`
- `Announcement`
- `FileRecord`
- `BlacklistRelation`

这些对象可在后续子类图或扩展类图中补充。
