# CampusHub API 规范文档

**阶段：** P3 详细设计  

---

## 1. 概述

### 1.1 基础信息

| 项目 | 说明 |
|------|------|
| 基础路径 | `/api` |
| 协议 | HTTPS |
| 数据格式 | JSON（请求/响应），文件上传使用 `multipart/form-data` |
| 字符编码 | UTF-8 |
| 认证方式 | JWT，Header: `Authorization: Bearer <token>` |
| WebSocket | `/ws/orders/{orderId}/chat`（订单内实时聊天） |

### 1.2 统一响应格式

**成功响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

**错误响应：**

```json
{
  "code": 40001,
  "message": "当前用户未完成校园身份认证",
  "data": null
}
```

`code` 为 0 表示成功；非 0 表示错误，具体含义见附录错误码表。

### 1.3 分页格式

**请求参数：**

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| page | integer | 1 | 页码，从 1 开始 |
| size | integer | 20 | 每页条数，最大 50 |

**响应格式：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 100,
    "page": 1,
    "size": 20,
    "pages": 5,
    "records": []
  }
}
```

### 1.4 认证说明

- `公开` — 无需认证即可访问
- `登录` — 需要携带有效 JWT Token
- `认证` — 需要登录且完成校园身份认证
- `管理员` — 需要管理员角色

### 1.5 枚举值定义

**UserRole：** `STUDENT`（学生）、`ADMIN`（管理员）  
**UserStatus：** `ACTIVE`（正常）、`DISABLED`（禁用）、`ANONYMIZED`（已注销）  
**TaskCategory：** `EXPRESS`（快递代取）、`ERRAND`（跑腿代办）、`TUTORING`（学习辅导）、`SECOND_HAND`（二手交易）、`LOST_FOUND`（失物招领）、`CONSULTATION`（咨询问答）、`TEAM_UP`（组队搭子）、`OTHER`（其他）  
**TaskStatus：** `OPEN`（待接单）、`IN_PROGRESS`（进行中）、`COMPLETED`（已完成）、`CANCELLED`（已取消）、`EXPIRED`（已过期）  
**ApplicationStatus：** `PENDING`（待确认）、`APPROVED`（已确认）、`REJECTED`（已拒绝）、`CANCELLED`（已取消）  
**OrderStatus：** `PENDING_CONFIRM`（待确认）、`IN_PROGRESS`（进行中）、`PENDING_COMPLETION`（待确认完成）、`COMPLETED`（已完成）、`CANCELLED`（已取消）、`TIMEOUT`（已超时）、`DISPUTE`（争议处理中）、`REVIEWED`（已评价）  
**RewardType：** `CASH`（现金）、`NEGOTIABLE`（面议）、`CREDIT_INTENT`（积分意向）  
**ReportTargetType：** `TASK`、`ORDER_MESSAGE`、`REVIEW`、`USER`  
**ReportReasonType：** `FRAUD`（诈骗）、`ABUSE`（辱骂）、`SPAM`（垃圾信息）、`ILLEGAL`（违法内容）、`TIMEOUT`（订单超时）、`OTHER`（其他）  
**ReportStatus：** `PENDING`（待处理）、`PROCESSING`（处理中）、`RESOLVED`（已处理）、`REJECTED`（已驳回）  
**MessageType：** `TEXT`（文字）、`IMAGE`（图片）  
**NotificationType：** `APPLICATION`（接单申请）、`ORDER_STATUS`（订单状态变更）、`ORDER_MESSAGE`（订单消息）、`REVIEW_REQUEST`（评价邀请）、`REPORT_RESULT`（举报结果）

---

## 2. 认证与用户模块

### 2.1 注册

**POST** `/api/auth/register` | 认证：公开

**请求体：**

```json
{
  "email": "student@smail.nju.edu.cn",
  "password": "Abc123456!",
  "confirmPassword": "Abc123456!"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| email | string | 是 | 学校邮箱，须以 smail.nju.edu.cn 结尾 |
| password | string | 是 | 密码，8-32位，含大小写字母+数字 |
| confirmPassword | string | 是 | 须与 password 一致 |

**成功响应（code=0）：**

```json
{
  "code": 0,
  "message": "注册成功，请前往邮箱查收验证邮件",
  "data": {
    "userId": 10001,
    "email": "student@smail.nju.edu.cn"
  }
}
```

**错误示例：**
- `40010` — 邮箱已注册
- `40011` — 邮箱格式不符合学校要求
- `40012` — 密码格式不符合要求

---

### 2.2 邮箱验证

**POST** `/api/auth/verify-email` | 认证：公开

**请求体：**

```json
{
  "email": "student@smail.nju.edu.cn",
  "code": "A7X92K"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| email | string | 是 | 注册邮箱 |
| code | string | 是 | 邮件中的6位验证码 |

**成功响应：**

```json
{
  "code": 0,
  "message": "邮箱验证成功，身份认证已完成",
  "data": {
    "verified": true
  }
}
```

**错误示例：**
- `40020` — 验证码错误或已过期
- `40021` — 邮箱未注册

---

### 2.3 发送验证码（重发/忘记密码）

**POST** `/api/auth/send-verification-code` | 认证：公开

**请求体：**

```json
{
  "email": "student@smail.nju.edu.cn",
  "purpose": "RESET_PASSWORD"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| email | string | 是 | 学校邮箱 |
| purpose | string | 是 | `REGISTER`（注册验证）/ `RESET_PASSWORD`（重置密码） |

**成功响应：**

```json
{
  "code": 0,
  "message": "验证码已发送",
  "data": null
}
```

---

### 2.4 登录

**POST** `/api/auth/login` | 认证：公开

**请求体：**

```json
{
  "email": "student@smail.nju.edu.cn",
  "password": "Abc123456!"
}
```

**成功响应：**

```json
{
  "code": 0,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "user": {
      "id": 10001,
      "email": "s***@smail.nju.edu.cn",
      "role": "STUDENT",
      "status": "ACTIVE",
      "verified": true,
      "nickname": "小明",
      "avatarUrl": "/uploads/avatars/10001.jpg"
    }
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| token | string | JWT Token，有效期 24 小时 |
| tokenType | string | 固定为 "Bearer" |
| expiresIn | integer | Token 有效期（秒） |
| user.id | long | 用户ID |
| user.email | string | 脱敏邮箱 |
| user.role | string | 角色 |
| user.status | string | 账号状态 |
| user.verified | boolean | 是否已完成校园认证 |
| user.nickname | string | 昵称 |
| user.avatarUrl | string | 头像URL |

**错误示例：**
- `40030` — 邮箱或密码错误
- `40031` — 账号已被禁用
- `40032` — 账号已锁定，请10分钟后重试（连续5次登录失败触发）

---

### 2.5 退出登录

**POST** `/api/auth/logout` | 认证：登录

无请求体。

**成功响应：**

```json
{
  "code": 0,
  "message": "已退出登录",
  "data": null
}
```

---

### 2.6 重置密码

**POST** `/api/auth/reset-password` | 认证：公开

**请求体：**

```json
{
  "email": "student@smail.nju.edu.cn",
  "code": "A7X92K",
  "newPassword": "NewPass123!",
  "confirmNewPassword": "NewPass123!"
}
```

**成功响应：**

```json
{
  "code": 0,
  "message": "密码重置成功，请重新登录",
  "data": null
}
```

---

### 2.7 获取当前用户资料

**GET** `/api/users/me` | 认证：登录

**成功响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 10001,
    "email": "s***@smail.nju.edu.cn",
    "role": "STUDENT",
    "status": "ACTIVE",
    "verified": true,
    "profile": {
      "nickname": "小明",
      "avatarUrl": "/uploads/avatars/10001.jpg",
      "gender": "MALE",
      "grade": "2024",
      "college": "计算机科学与技术系",
      "bio": "热爱生活，喜欢帮助他人",
      "campus": "仙林校区",
      "contact": "QQ:12345678",
      "contactVisible": true
    },
    "credit": {
      "score": 100,
      "completedOrders": 5,
      "praiseRate": 0.96
    },
    "createdAt": "2026-04-20T10:00:00"
  }
}
```

---

### 2.8 修改当前用户资料

**PATCH** `/api/users/me` | 认证：认证

**请求体（全部可选）：**

```json
{
  "nickname": "小明同学",
  "avatarUrl": "/uploads/avatars/10001_v2.jpg",
  "gender": "MALE",
  "grade": "2024",
  "college": "计算机科学与技术系",
  "bio": "热爱生活，喜欢帮助他人",
  "campus": "仙林校区",
  "contact": "WeChat:xiaoming123",
  "contactVisible": false
}
```

**成功响应：** 返回更新后的完整用户资料（同 GET /api/users/me）

---

### 2.9 获取用户公开资料

**GET** `/api/users/{userId}/profile` | 认证：登录

**成功响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "userId": 10001,
    "nickname": "小明",
    "avatarUrl": "/uploads/avatars/10001.jpg",
    "gender": "MALE",
    "college": "计算机科学与技术系",
    "campus": "仙林校区",
    "verified": true,
    "contact": "WeChat:xiaoming123",
    "contactVisible": true,
    "creditScore": 100,
    "completedOrders": 5,
    "praiseRate": 0.96,
    "memberSince": "2026-04-20"
  }
}
```

注：`contact` 仅在 `contactVisible=true` 时返回实际内容，否则返回 null。

---

### 2.10 获取用户评价记录

**GET** `/api/users/{userId}/reviews` | 认证：登录

**查询参数：** `page`、`size`

**成功响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 12,
    "page": 1,
    "size": 20,
    "pages": 1,
    "records": [
      {
        "id": 2001,
        "orderId": 5001,
        "reviewerId": 10002,
        "reviewerNickname": "小红",
        "rating": 5,
        "content": "非常靠谱，包裹完好无损送达",
        "createdAt": "2026-05-10T14:30:00"
      }
    ]
  }
}
```

---

### 2.11 注销账号

**DELETE** `/api/users/me` | 认证：认证

**请求体：**

```json
{
  "password": "Abc123456!"
}
```

**成功响应：**

```json
{
  "code": 0,
  "message": "账号已注销",
  "data": null
}
```

---

## 3. 需求与任务大厅模块

### 3.1 任务大厅列表

**GET** `/api/tasks` | 认证：公开（游客/未认证用户可浏览公开需求）

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | integer | 否 | 页码，默认 1 |
| size | integer | 否 | 每页条数，默认 20 |
| category | string | 否 | 需求分类，见枚举 TaskCategory |
| campus | string | 否 | 校区筛选 |
| keyword | string | 否 | 关键词搜索（标题+描述） |
| sort | string | 否 | `latest`（最新发布，默认）/ `deadline`（截止时间） |
| status | string | 否 | `OPEN`（默认仅展示待接单）/ 管理员可查看全部 |

**成功响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 45,
    "page": 1,
    "size": 20,
    "pages": 3,
    "records": [
      {
        "id": 3001,
        "publisherId": 10001,
        "publisherNickname": "小明",
        "category": "EXPRESS",
        "title": "帮忙取一下韵达快递",
        "description": "韵达快递，取件码 A-3-2105，在仙林校区快递点",
        "campus": "仙林校区",
        "rewardType": "CASH",
        "deadline": "2026-05-18T18:00:00",
        "status": "OPEN",
        "anonymous": false,
        "imageUrls": ["/uploads/tasks/3001_1.jpg"],
        "applicationCount": 2,
        "favoriteCount": 3,
        "isFavorited": false,
        "createdAt": "2026-05-17T10:00:00"
      }
    ]
  }
}
```

---

### 3.2 获取需求详情

**GET** `/api/tasks/{taskId}` | 认证：公开（游客/未认证用户可查看公开需求详情）

**成功响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 3001,
    "publisherId": 10001,
    "publisherNickname": "小明",
    "publisherAvatarUrl": "/uploads/avatars/10001.jpg",
    "category": "EXPRESS",
    "title": "帮忙取一下韵达快递",
    "description": "韵达快递，取件码 A-3-2105，在仙林校区快递点",
    "campus": "仙林校区",
    "rewardType": "CASH",
    "deadline": "2026-05-18T18:00:00",
    "status": "OPEN",
    "anonymous": false,
    "categoryFields": {
      "expressCompany": "韵达快递",
      "pickupLocation": "仙林校区快递点",
      "pickupCode": "A-3-2105",
      "deliveryLocation": "仙林校区12栋"
    },
    "images": [
      {
        "id": 4001,
        "url": "/uploads/tasks/3001_1.jpg",
        "sortOrder": 1
      }
    ],
    "applicationCount": 2,
    "favoriteCount": 3,
    "isFavorited": false,
    "createdAt": "2026-05-17T10:00:00",
    "updatedAt": "2026-05-17T10:30:00"
  }
}
```

注：`categoryFields` 按需求类别返回差异化字段。`pickupCode` 仅在订单确认后对接单者展示完整内容，否则脱敏或隐藏。

---

### 3.3 发布需求

**POST** `/api/tasks` | 认证：认证

**请求体：**

```json
{
  "category": "EXPRESS",
  "title": "帮忙取一下韵达快递",
  "description": "韵达快递，取件码 A-3-2105，在仙林校区快递点，送到12栋楼下",
  "campus": "仙林校区",
  "rewardType": "CASH",
  "deadline": "2026-05-18T18:00:00",
  "anonymous": false,
  "imageIds": [4001],
  "categoryFields": {
    "expressCompany": "韵达快递",
    "pickupLocation": "仙林校区快递点",
    "pickupCode": "A-3-2105",
    "deliveryLocation": "仙林校区12栋"
  }
}
```

| 通用字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| category | string | 是 | 需求分类 |
| title | string | 是 | 标题，2-100字符 |
| description | string | 是 | 描述，10-2000字符 |
| campus | string | 是 | 校区/地点 |
| rewardType | string | 是 | 报酬类型 |
| deadline | datetime | 是 | 截止时间，必须在当前时间之后 |
| anonymous | boolean | 否 | 是否匿名发布，默认 false |
| imageIds | long[] | 否 | 已上传的图片ID列表，最多9张 |

**分类差异化字段（categoryFields）：**

| 分类 | 专属字段 | 类型 | 必填 |
|------|---------|------|------|
| EXPRESS | expressCompany | string | 是 |
| | pickupLocation | string | 是 |
| | pickupCode | string | 是 |
| | deliveryLocation | string | 是 |
| SECOND_HAND | goodsCategory | string | 是 |
| | condition | string | 是（NEW/LIKE_NEW/USED） |
| | price | decimal | 是 |
| LOST_FOUND | itemName | string | 是 |
| | location | string | 是 |
| | foundTime | datetime | 是 |
| | itemDescription | string | 是 |
| | contactInfo | string | 是 |
| TEAM_UP | activityType | string | 是 |
| | requiredCount | integer | 是 |
| | activityTime | datetime | 是 |

**成功响应：**

```json
{
  "code": 0,
  "message": "需求发布成功",
  "data": {
    "id": 3001,
    "status": "OPEN",
    "createdAt": "2026-05-17T10:00:00"
  }
}
```

**错误示例：**
- `40100` — 未完成校园身份认证
- `40101` — 标题或描述包含违规内容
- `40102` — 分类专属字段不完整

---

### 3.4 编辑需求

**PATCH** `/api/tasks/{taskId}` | 认证：认证

仅发布者可操作，且需求必须处于 `OPEN` 状态。

**请求体：** 同发布需求，全部字段可选。

**成功响应：** 返回更新后的需求详情（同 GET /api/tasks/{taskId}）

**错误示例：**
- `40110` — 需求已被接单，不可编辑
- `40111` — 无权操作（非发布者）

---

### 3.5 删除需求

**DELETE** `/api/tasks/{taskId}` | 认证：认证

仅发布者可操作，且需求必须处于 `OPEN` 状态。

**成功响应：**

```json
{
  "code": 0,
  "message": "需求已删除",
  "data": null
}
```

---

### 3.6 收藏/取消收藏需求

**POST** `/api/tasks/{taskId}/favorite` | 认证：认证

无请求体。如果已收藏则取消收藏，如果未收藏则添加收藏。

**成功响应（收藏）：**

```json
{
  "code": 0,
  "message": "已收藏",
  "data": { "favorited": true }
}
```

**成功响应（取消收藏）：**

```json
{
  "code": 0,
  "message": "已取消收藏",
  "data": { "favorited": false }
}
```

---

### 3.7 获取我的收藏

**GET** `/api/tasks/favorites` | 认证：登录

**查询参数：** `page`、`size`

**成功响应：** 分页返回收藏的需求列表（同任务大厅列表项格式）

---

## 4. 接单与订单模块

### 4.1 发起接单申请

**POST** `/api/tasks/{taskId}/applications` | 认证：认证

**请求体：**

```json
{
  "message": "我住在12栋，可以帮你取快递，下午3点有空"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| message | string | 是 | 申请留言，1-500字符 |

**成功响应：**

```json
{
  "code": 0,
  "message": "接单申请已提交",
  "data": {
    "applicationId": 6001,
    "taskId": 3001,
    "status": "PENDING",
    "createdAt": "2026-05-17T11:00:00"
  }
}
```

**错误示例：**
- `40200` — 需求已过期或已被接单
- `40201` — 不能申请自己的需求
- `40202` — 信用分不足（低于60分限制接单）
- `40203` — 账号已被限制接单

---

### 4.2 查看需求的所有接单申请

**GET** `/api/tasks/{taskId}/applications` | 认证：认证

仅需求发布者可查看。

**成功响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 6001,
      "applicantId": 10002,
      "applicantNickname": "小红",
      "applicantAvatarUrl": "/uploads/avatars/10002.jpg",
      "applicantCreditScore": 95,
      "message": "我住在12栋，可以帮你取快递",
      "status": "PENDING",
      "createdAt": "2026-05-17T11:00:00"
    }
  ]
}
```

---

### 4.3 确认接单（发布者确认服务方）

**POST** `/api/applications/{applicationId}/confirm` | 认证：认证

仅需求发布者可操作。确认后自动拒绝其他申请，并创建订单。

**成功响应：**

```json
{
  "code": 0,
  "message": "已确认接单，订单已生成",
  "data": {
    "orderId": 7001,
    "taskId": 3001,
    "status": "IN_PROGRESS",
    "publisherId": 10001,
    "serviceProviderId": 10002,
    "createdAt": "2026-05-17T12:00:00"
  }
}
```

---

### 4.4 拒绝接单申请

**POST** `/api/applications/{applicationId}/reject` | 认证：认证

仅需求发布者可操作。

**请求体：**

```json
{
  "reason": "时间不合适"
}
```

**成功响应：**

```json
{
  "code": 0,
  "message": "已拒绝该申请",
  "data": null
}
```

---

### 4.5 获取我的订单列表

**GET** `/api/orders` | 认证：登录

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | integer | 否 | 页码 |
| size | integer | 否 | 每页条数 |
| role | string | 否 | `PUBLISHER`（我发布的）/ `PROVIDER`（我接单的），不传返回全部 |
| status | string | 否 | 按订单状态筛选 |
| keyword | string | 否 | 按需求标题搜索 |

**成功响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 8,
    "page": 1,
    "size": 20,
    "pages": 1,
    "records": [
      {
        "id": 7001,
        "taskId": 3001,
        "taskTitle": "帮忙取一下韵达快递",
        "publisherId": 10001,
        "publisherNickname": "小明",
        "serviceProviderId": 10002,
        "serviceProviderNickname": "小红",
        "status": "IN_PROGRESS",
        "createdAt": "2026-05-17T12:00:00"
      }
    ]
  }
}
```

---

### 4.6 获取订单详情

**GET** `/api/orders/{orderId}` | 认证：认证

仅订单双方可查看。

**成功响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 7001,
    "taskId": 3001,
    "taskTitle": "帮忙取一下韵达快递",
    "taskCategory": "EXPRESS",
    "taskDeadline": "2026-05-18T18:00:00",
    "publisher": {
      "id": 10001,
      "nickname": "小明",
      "avatarUrl": "/uploads/avatars/10001.jpg",
      "contact": "WeChat:xiaoming123",
      "contactVisible": true
    },
    "serviceProvider": {
      "id": 10002,
      "nickname": "小红",
      "avatarUrl": "/uploads/avatars/10002.jpg",
      "contact": "QQ:87654321",
      "contactVisible": true
    },
    "status": "IN_PROGRESS",
    "completionProofUrl": null,
    "cancelReason": null,
    "createdAt": "2026-05-17T12:00:00",
    "canBeReviewed": false
  }
}
```

---

### 4.7 服务方提交完成凭证

**POST** `/api/orders/{orderId}/complete` | 认证：认证

仅服务方在 `IN_PROGRESS` 状态可操作。

**请求体：**

```json
{
  "proofImageId": 8001,
  "note": "快递已放在12栋快递架"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| proofImageId | long | 是 | 已上传的凭证图片ID |
| note | string | 否 | 完成备注，最多500字符 |

**成功响应：**

```json
{
  "code": 0,
  "message": "完成凭证已提交，等待发布者确认",
  "data": {
    "orderId": 7001,
    "status": "PENDING_COMPLETION"
  }
}
```

---

### 4.8 发布者确认完成

**POST** `/api/orders/{orderId}/confirm-completion` | 认证：认证

仅发布者在 `PENDING_COMPLETION` 状态可操作。订单进入 `COMPLETED` 状态，开启评价入口。

**成功响应：**

```json
{
  "code": 0,
  "message": "已完成确认，请对本次服务进行评价",
  "data": {
    "orderId": 7001,
    "status": "COMPLETED",
    "canBeReviewed": true
  }
}
```

---

### 4.9 取消订单

**POST** `/api/orders/{orderId}/cancel` | 认证：认证

**请求体：**

```json
{
  "reason": "双方协商一致取消"
}
```

**状态约束：**
- `PENDING_CONFIRM` 状态：发布者可直接取消
- `IN_PROGRESS` 状态：任一方可发起取消，需填写原因；24小时未处理自动取消
- `COMPLETED` / `REVIEWED` 状态：不可取消

**成功响应：**

```json
{
  "code": 0,
  "message": "订单已取消",
  "data": {
    "orderId": 7001,
    "status": "CANCELLED"
  }
}
```

---

### 4.10 发起争议

**POST** `/api/orders/{orderId}/dispute` | 认证：认证

在 `PENDING_COMPLETION` 状态，发布者可发起争议。

**请求体：**

```json
{
  "reason": "快递包裹有破损，与描述不符",
  "evidenceImageIds": [9001, 9002]
}
```

**成功响应：**

```json
{
  "code": 0,
  "message": "争议已提交，管理员将介入处理",
  "data": {
    "orderId": 7001,
    "status": "DISPUTE"
  }
}
```

---

### 4.11 获取订单状态日志

**GET** `/api/orders/{orderId}/status-logs` | 认证：认证

仅订单双方可查看。

**成功响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 10001,
      "fromStatus": "PENDING_CONFIRM",
      "toStatus": "IN_PROGRESS",
      "operatorId": 10001,
      "reason": null,
      "createdAt": "2026-05-17T12:00:00"
    }
  ]
}
```

---

## 5. 消息通知模块

### 5.1 获取通知列表

**GET** `/api/notifications` | 认证：登录

**查询参数：** `page`、`size`、`type`（通知类型筛选）、`read`（是否已读）

**成功响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 25,
    "page": 1,
    "size": 20,
    "pages": 2,
    "records": [
      {
        "id": 11001,
        "type": "APPLICATION",
        "title": "新的接单申请",
        "content": "小红 申请接取你的需求「帮忙取一下韵达快递」",
        "read": false,
        "relatedOrderId": null,
        "relatedTaskId": 3001,
        "createdAt": "2026-05-17T11:00:00"
      }
    ]
  }
}
```

---

### 5.2 获取未读通知数

**GET** `/api/notifications/unread-count` | 认证：登录

**成功响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "count": 5
  }
}
```

---

### 5.3 标记通知为已读

**PATCH** `/api/notifications/{notificationId}/read` | 认证：登录

**成功响应：**

```json
{
  "code": 0,
  "message": "已标记为已读",
  "data": null
}
```

---

### 5.4 全部标记为已读

**PATCH** `/api/notifications/read-all` | 认证：登录

**成功响应：**

```json
{
  "code": 0,
  "message": "已全部标记为已读",
  "data": { "count": 5 }
}
```

---

### 5.5 删除通知

**DELETE** `/api/notifications/{notificationId}` | 认证：登录

**成功响应：**

```json
{
  "code": 0,
  "message": "通知已删除",
  "data": null
}
```

---

### 5.6 获取订单聊天消息

**GET** `/api/orders/{orderId}/messages` | 认证：认证

仅订单双方可查看。

**查询参数：** `page`、`size`（默认20，消息按时间倒序）

**成功响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 30,
    "page": 1,
    "size": 20,
    "pages": 2,
    "records": [
      {
        "id": 12001,
        "senderId": 10001,
        "senderNickname": "小明",
        "senderAvatarUrl": "/uploads/avatars/10001.jpg",
        "messageType": "TEXT",
        "content": "好的，谢谢！",
        "imageUrl": null,
        "createdAt": "2026-05-17T12:15:00"
      },
      {
        "id": 12000,
        "senderId": 10002,
        "senderNickname": "小红",
        "senderAvatarUrl": "/uploads/avatars/10002.jpg",
        "messageType": "IMAGE",
        "content": null,
        "imageUrl": "/uploads/messages/12000.jpg",
        "createdAt": "2026-05-17T12:10:00"
      }
    ]
  }
}
```

---

### 5.7 发送文字消息

**POST** `/api/orders/{orderId}/messages` | 认证：认证

仅订单双方可操作。

**请求体：**

```json
{
  "messageType": "TEXT",
  "content": "快递已经帮你取到了"
}
```

**成功响应：**

```json
{
  "code": 0,
  "message": "消息已发送",
  "data": {
    "id": 12002,
    "messageType": "TEXT",
    "content": "快递已经帮你取到了",
    "createdAt": "2026-05-17T12:20:00"
  }
}
```

注：发送成功后通过 WebSocket 推送给订单另一方。

---

### 5.8 发送图片消息

**POST** `/api/orders/{orderId}/messages/image` | 认证：认证

仅订单双方可操作。

**请求体：** `multipart/form-data`

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| image | file | 是 | 图片文件，最大 5MB，格式 jpg/png/gif |
| content | string | 否 | 附带的文字说明 |

**成功响应：**

```json
{
  "code": 0,
  "message": "图片消息已发送",
  "data": {
    "id": 12003,
    "messageType": "IMAGE",
    "imageUrl": "/uploads/messages/12003.jpg",
    "content": "快递照片",
    "createdAt": "2026-05-17T12:25:00"
  }
}
```

---

### 5.9 WebSocket 实时聊天

**连接：** `ws://<host>/ws/orders/{orderId}/chat?token=<JWT>`

连接时携带 JWT Token 作为查询参数进行鉴权。服务端校验用户必须是订单双方之一。

**服务端推送事件格式：**

```json
{
  "event": "NEW_MESSAGE",
  "data": {
    "id": 12002,
    "senderId": 10001,
    "senderNickname": "小明",
    "senderAvatarUrl": "/uploads/avatars/10001.jpg",
    "messageType": "TEXT",
    "content": "好的，谢谢！",
    "imageUrl": null,
    "createdAt": "2026-05-17T12:20:00"
  }
}
```

| 事件 | 说明 |
|------|------|
| NEW_MESSAGE | 新消息到达（文字或图片） |
| ORDER_STATUS_CHANGE | 订单状态变更通知 |

---

## 6. 评价与信用模块

### 6.1 提交评价

**POST** `/api/orders/{orderId}/reviews` | 认证：认证

订单处于 `COMPLETED` 状态后 7 天内可提交。双方均可评价。

**请求体：**

```json
{
  "rating": 5,
  "content": "非常靠谱，包裹完好无损送达，速度快态度好"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| rating | integer | 是 | 评分 1-5 |
| content | string| 否 | 评价内容，最多500字符 |

**成功响应：**

```json
{
  "code": 0,
  "message": "评价提交成功",
  "data": {
    "reviewId": 13001,
    "orderId": 7001,
    "rating": 5,
    "createdAt": "2026-05-17T15:00:00"
  }
}
```

**错误示例：**
- `40400` — 订单未完成，不可评价
- `40401` — 评价已存在，不可重复评价
- `40402` — 已超过7天评价期限

---

### 6.2 获取订单评价

**GET** `/api/orders/{orderId}/reviews` | 认证：登录

**成功响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 13001,
      "reviewerId": 10001,
      "reviewerNickname": "小明",
      "revieweeId": 10002,
      "revieweeNickname": "小红",
      "rating": 5,
      "content": "非常靠谱，包裹完好无损送达",
      "createdAt": "2026-05-17T15:00:00"
    },
    {
      "id": 13002,
      "reviewerId": 10002,
      "reviewerNickname": "小红",
      "revieweeId": 10001,
      "revieweeNickname": "小明",
      "rating": 5,
      "content": "沟通顺畅，确认及时",
      "createdAt": "2026-05-17T15:10:00"
    }
  ]
}
```

---

### 6.3 获取用户信用信息

**GET** `/api/users/{userId}/credit` | 认证：登录

**成功响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "userId": 10001,
    "score": 100,
    "completedOrders": 5,
    "praiseRate": 0.96,
    "recentReviews": [
      {
        "id": 13003,
        "orderId": 5002,
        "reviewerNickname": "小刚",
        "rating": 4,
        "content": "还不错",
        "createdAt": "2026-05-10T10:00:00"
      }
    ]
  }
}
```

---

## 7. 举报模块

### 7.1 提交举报

**POST** `/api/tasks/{taskId}/reports` | 认证：认证

提交任务举报。当前实现另提供 **POST** `/api/users/{userId}/reports` 用于举报用户账号；订单消息和评价举报属于预留目标类型，当前前后端入口尚未开放。

**请求体：**

```json
{
  "reason": "该需求重复发布，已发布过完全相同的快递代取需求",
  "evidenceImageIds": [9001, 9002]
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| reason | string | 是 | 举报原因或补充说明 |
| evidenceImageIds | long[] | 否 | 已上传的举报证据图片文件 ID |

**成功响应：**

```json
{
  "code": 0,
  "message": "举报已提交",
  "data": {
    "reportId": 14001,
    "taskId": 3001,
    "reason": "该需求重复发布，已发布过完全相同的快递代取需求",
    "evidenceImageIds": [9001, 9002],
    "createdAt": "2026-05-17T16:00:00"
  }
}
```

---

### 7.2 获取我的举报列表

**GET** `/api/reports` | 认证：登录

**查询参数：** `page`、`size`、`status`

**成功响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 3,
    "page": 1,
    "size": 20,
    "pages": 1,
    "records": [
      {
        "id": 14001,
        "targetType": "TASK",
        "targetId": 3001,
        "reasonType": "SPAM",
        "description": "该需求重复发布...",
        "status": "PENDING",
        "result": null,
        "createdAt": "2026-05-17T16:00:00"
      }
    ]
  }
}
```

---

### 7.3 获取举报详情

**GET** `/api/reports/{reportId}` | 认证：认证

仅举报人可查看。

**成功响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 14001,
    "reporterId": 10001,
    "targetType": "TASK",
    "targetId": 3001,
    "reasonType": "SPAM",
    "description": "该需求重复发布...",
    "status": "RESOLVED",
    "result": "举报成立，已下架该需求",
    "processedBy": 20001,
    "processedAt": "2026-05-17T18:00:00",
    "createdAt": "2026-05-17T16:00:00"
  }
}
```

---

## 8. 文件上传模块

### 8.1 上传图片

**POST** `/api/files/upload` | 认证：认证

**请求体：** `multipart/form-data`

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | file | 是 | 图片文件，最大 10MB，格式 jpg/png/gif/webp |
| businessType | string | 是 | 用途：`AVATAR` / `TASK_IMAGE` / `ORDER_PROOF` / `CHAT_IMAGE` / `REPORT_EVIDENCE` |

**成功响应：**

```json
{
  "code": 0,
  "message": "上传成功",
  "data": {
    "fileId": 15001,
    "url": "/uploads/images/2026/05/15001.jpg",
    "fileName": "task_image.jpg",
    "fileSize": 204800
  }
}
```

**错误示例：**
- `40500` — 文件类型不支持
- `40501` — 文件大小超出限制
- `40502` — 上传失败，请重试

---

## 9. 后台管理模块

后台管理接口均需 `ADMIN` 角色。

### 9.1 管理后台概览

**GET** `/api/admin/dashboard` | 认证：管理员

**成功响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "totalUsers": 150,
    "totalTasks": 320,
    "totalOrders": 180,
    "pendingReports": 5,
    "activeUsersToday": 42,
    "newUsersToday": 8,
    "newOrdersToday": 12
  }
}
```

---

### 9.2 用户管理列表

**GET** `/api/admin/users` | 认证：管理员

**查询参数：** `page`、`size`、`keyword`（邮箱/昵称搜索）、`status`、`verified`

**成功响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 150,
    "page": 1,
    "size": 20,
    "pages": 8,
    "records": [
      {
        "id": 10001,
        "email": "s***@smail.nju.edu.cn",
        "nickname": "小明",
        "role": "STUDENT",
        "status": "ACTIVE",
        "verified": true,
        "creditScore": 100,
        "createdAt": "2026-04-20T10:00:00"
      }
    ]
  }
}
```

---

### 9.3 获取用户详情（管理员视角）

**GET** `/api/admin/users/{userId}` | 认证：管理员

**成功响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 10001,
    "email": "student@smail.nju.edu.cn",
    "nickname": "小明",
    "role": "STUDENT",
    "status": "ACTIVE",
    "verified": true,
    "creditScore": 100,
    "publishedTaskCount": 10,
    "acceptedOrderCount": 5,
    "reportCount": 0,
    "createdAt": "2026-04-20T10:00:00"
  }
}
```

---

### 9.4 禁用/解禁用户

**PATCH** `/api/admin/users/{userId}/status` | 认证：管理员

**请求体：**

```json
{
  "status": "DISABLED",
  "reason": "多次发布违规内容"
}
```

**成功响应：**

```json
{
  "code": 0,
  "message": "用户状态已更新",
  "data": {
    "userId": 10001,
    "status": "DISABLED"
  }
}
```

---

### 9.5 需求管理列表

**GET** `/api/admin/tasks` | 认证：管理员

**查询参数：** `page`、`size`、`keyword`、`status`、`category`

**成功响应格式同 3.1 任务大厅列表**，但额外包含 `publisherEmail` 字段。

---

### 9.6 下架/恢复需求

**PATCH** `/api/admin/tasks/{taskId}/status` | 认证：管理员

**请求体：**

```json
{
  "status": "CANCELLED",
  "reason": "包含违规内容"
}
```

**成功响应：**

```json
{
  "code": 0,
  "message": "需求状态已更新",
  "data": {
    "taskId": 3001,
    "status": "CANCELLED"
  }
}
```

---

### 9.7 订单管理列表

**GET** `/api/admin/orders` | 认证：管理员

**查询参数：** `page`、`size`、`status`、`keyword`

**成功响应格式同 4.5**，额外包含双方邮箱。

---

### 9.8 冻结/恢复订单

**PATCH** `/api/admin/orders/{orderId}/status` | 认证：管理员

**请求体：**

```json
{
  "status": "CANCELLED",
  "reason": "违规订单"
}
```

---

### 9.9 举报管理列表

**GET** `/api/admin/reports` | 认证：管理员

**查询参数：** `page`、`size`、`status`

**成功响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 25,
    "page": 1,
    "size": 20,
    "pages": 2,
    "records": [
      {
        "id": 14001,
        "reporterId": 10001,
        "reporterNickname": "小明",
        "targetType": "TASK",
        "targetId": 3001,
        "reasonType": "SPAM",
        "description": "该需求重复发布...",
        "status": "PENDING",
        "createdAt": "2026-05-17T16:00:00"
      }
    ]
  }
}
```

---

### 9.10 处理举报

**PATCH** `/api/admin/reports/{reportId}` | 认证：管理员

**请求体：**

```json
{
  "status": "RESOLVED",
  "result": "举报成立，已下架该需求并警告发布者"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | string | 是 | `RESOLVED`（处理完成）/ `REJECTED`（驳回举报） |
| result | string | 是 | 处理说明，1-500字符 |

**成功响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

---

### 9.11 发布公告

**POST** `/api/admin/announcements` | 认证：管理员

**请求体：**

```json
{
  "title": "关于平台维护的通知",
  "content": "平台将于2026年5月20日凌晨2:00-4:00进行维护，届时将暂停服务。",
  "priority": "NORMAL"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | string | 是 | 标题，1-100字符 |
| content | string | 是 | 内容，1-5000字符 |
| priority | string | 否 | 优先级：`NORMAL` / `IMPORTANT`，默认 NORMAL |

**成功响应：**

```json
{
  "code": 0,
  "message": "公告已发布",
  "data": {
    "announcementId": 16001,
    "createdAt": "2026-05-17T20:00:00"
  }
}
```

---

### 9.12 公告列表

**GET** `/api/admin/announcements` | 认证：管理员

**查询参数：** `page`、`size`

**成功响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 5,
    "page": 1,
    "size": 20,
    "pages": 1,
    "records": [
      {
        "id": 16001,
        "title": "关于平台维护的通知",
        "content": "平台将于2026年5月20日...",
        "priority": "NORMAL",
        "publisherId": 20001,
        "publisherNickname": "管理员",
        "createdAt": "2026-05-17T20:00:00"
      }
    ]
  }
}
```

---

### 9.13 编辑公告

**PATCH** `/api/admin/announcements/{announcementId}` | 认证：管理员

请求体同 9.11（全部可选）。

---

### 9.14 删除公告

**DELETE** `/api/admin/announcements/{announcementId}` | 认证：管理员

---

## 10. 公开接口

### 10.1 前台公告列表

**GET** `/api/announcements` | 认证：公开

响应格式同 9.12，返回最近公告列表。

---

## 附录 A：错误码表

### 通用错误码

| code | 说明 |
|------|------|
| 0 | 成功 |
| 40000 | 参数校验失败 |
| 40001 | 未登录或 Token 已过期 |
| 40002 | 未完成校园身份认证 |
| 40003 | 权限不足 |
| 40004 | 资源不存在 |
| 50000 | 服务器内部错误 |

### 认证模块（40010-40039）

| code | 说明 |
|------|------|
| 40010 | 邮箱已注册 |
| 40011 | 邮箱格式不符合学校要求 |
| 40012 | 密码格式不符合要求 |
| 40020 | 验证码错误或已过期 |
| 40021 | 邮箱未注册 |
| 40030 | 邮箱或密码错误 |
| 40031 | 账号已被禁用 |
| 40032 | 账号已锁定，请10分钟后重试 |

### 需求模块（40100-40119）

| code | 说明 |
|------|------|
| 40100 | 未完成校园身份认证，不可发布需求 |
| 40101 | 标题或描述包含违规内容 |
| 40102 | 分类专属字段不完整或不合法 |
| 40110 | 需求已被接单，不可编辑或删除 |
| 40111 | 无权操作该需求 |

### 订单模块（40200-40219）

| code | 说明 |
|------|------|
| 40200 | 需求已过期或已被接单 |
| 40201 | 不能申请自己的需求 |
| 40202 | 信用分不足，限制接单 |
| 40203 | 账号已被限制接单 |
| 40210 | 订单状态不允许该操作 |
| 40211 | 不是订单参与方，无权操作 |

### 评价模块（40400-40409）

| code | 说明 |
|------|------|
| 40400 | 订单未完成，不可评价 |
| 40401 | 已评价，不可重复评价 |
| 40402 | 已超过评价期限 |

### 文件模块（40500-40509）

| code | 说明 |
|------|------|
| 40500 | 文件类型不支持 |
| 40501 | 文件大小超出限制 |
| 40502 | 上传失败，请重试 |

---

## 附录 B：HTTP 状态码使用约定

| HTTP Status | 场景 |
|-------------|------|
| 200 | 所有正常响应（业务错误通过 body.code 区分） |
| 401 | Token 缺失、无效或已过期 |
| 403 | Token 有效但权限不足（如非管理员访问后台接口） |
| 404 | 请求路径不存在 |
| 500 | 服务器未捕获异常 |

---

## 附录 C：接口总览

| 序号 | 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|------|
| 1 | POST | `/api/auth/register` | 公开 | 注册 |
| 2 | POST | `/api/auth/verify-email` | 公开 | 邮箱验证 |
| 3 | POST | `/api/auth/send-verification-code` | 公开 | 发送验证码 |
| 4 | POST | `/api/auth/login` | 公开 | 登录 |
| 5 | POST | `/api/auth/logout` | 登录 | 退出登录 |
| 6 | POST | `/api/auth/reset-password` | 公开 | 重置密码 |
| 7 | GET | `/api/users/me` | 登录 | 获取当前用户资料 |
| 8 | PATCH | `/api/users/me` | 认证 | 修改个人资料 |
| 9 | GET | `/api/users/{userId}/profile` | 登录 | 获取用户公开资料 |
| 10 | GET | `/api/users/{userId}/reviews` | 登录 | 获取用户评价记录 |
| 11 | DELETE | `/api/users/me` | 认证 | 注销账号 |
| 12 | GET | `/api/tasks` | 公开 | 任务大厅列表 |
| 13 | GET | `/api/tasks/{taskId}` | 公开 | 需求详情 |
| 14 | POST | `/api/tasks` | 认证 | 发布需求 |
| 15 | PATCH | `/api/tasks/{taskId}` | 认证 | 编辑需求 |
| 16 | DELETE | `/api/tasks/{taskId}` | 认证 | 删除需求 |
| 17 | POST | `/api/tasks/{taskId}/favorite` | 认证 | 收藏/取消收藏 |
| 18 | GET | `/api/tasks/favorites` | 登录 | 我的收藏 |
| 19 | POST | `/api/tasks/{taskId}/applications` | 认证 | 发起接单申请 |
| 20 | GET | `/api/tasks/{taskId}/applications` | 认证 | 查看接单申请 |
| 21 | POST | `/api/applications/{applicationId}/confirm` | 认证 | 确认接单 |
| 22 | POST | `/api/applications/{applicationId}/reject` | 认证 | 拒绝申请 |
| 23 | GET | `/api/orders` | 登录 | 我的订单列表 |
| 24 | GET | `/api/orders/{orderId}` | 认证 | 订单详情 |
| 25 | POST | `/api/orders/{orderId}/complete` | 认证 | 提交完成凭证 |
| 26 | POST | `/api/orders/{orderId}/confirm-completion` | 认证 | 确认完成 |
| 27 | POST | `/api/orders/{orderId}/cancel` | 认证 | 取消订单 |
| 28 | POST | `/api/orders/{orderId}/dispute` | 认证 | 发起争议 |
| 29 | GET | `/api/orders/{orderId}/status-logs` | 认证 | 订单状态日志 |
| 30 | GET | `/api/notifications` | 登录 | 通知列表 |
| 31 | GET | `/api/notifications/unread-count` | 登录 | 未读通知数 |
| 32 | PATCH | `/api/notifications/{notificationId}/read` | 登录 | 标记已读 |
| 33 | PATCH | `/api/notifications/read-all` | 登录 | 全部已读 |
| 34 | DELETE | `/api/notifications/{id}` | 登录 | 删除通知 |
| 35 | GET | `/api/orders/{orderId}/messages` | 认证 | 订单聊天记录 |
| 36 | POST | `/api/orders/{orderId}/messages` | 认证 | 发送文字消息 |
| 37 | POST | `/api/orders/{orderId}/messages/image` | 认证 | 发送图片消息 |
| 38 | WS | `/ws/orders/{orderId}/chat` | 认证 | WebSocket 实时聊天 |
| 39 | POST | `/api/orders/{orderId}/reviews` | 认证 | 提交评价 |
| 40 | GET | `/api/orders/{orderId}/reviews` | 登录 | 查看订单评价 |
| 41 | GET | `/api/users/{userId}/credit` | 登录 | 查看信用信息 |
| 42 | POST | `/api/tasks/{taskId}/reports` | 认证 | 举报任务 |
| 43 | POST | `/api/users/{userId}/reports` | 认证 | 举报用户 |
| 44 | GET | `/api/reports` | 登录 | 我的举报列表 |
| 45 | GET | `/api/reports/{reportId}` | 认证 | 举报详情 |
| 46 | POST | `/api/files/upload` | 认证 | 上传图片 |
| 47 | GET | `/api/admin/dashboard` | 管理员 | 后台概览 |
| 48 | GET | `/api/admin/users` | 管理员 | 用户管理列表 |
| 49 | GET | `/api/admin/users/{userId}` | 管理员 | 用户详情 |
| 50 | PATCH | `/api/admin/users/{userId}/status` | 管理员 | 禁用/解禁用户 |
| 51 | GET | `/api/admin/tasks` | 管理员 | 需求管理列表 |
| 52 | PATCH | `/api/admin/tasks/{taskId}/status` | 管理员 | 下架/恢复需求 |
| 53 | GET | `/api/admin/orders` | 管理员 | 订单管理列表 |
| 54 | PATCH | `/api/admin/orders/{orderId}/status` | 管理员 | 冻结/恢复订单 |
| 55 | GET | `/api/admin/reports` | 管理员 | 举报管理列表 |
| 56 | PATCH | `/api/admin/reports/{reportId}` | 管理员 | 处理举报 |
| 57 | POST | `/api/admin/announcements` | 管理员 | 发布公告 |
| 58 | GET | `/api/admin/announcements` | 管理员 | 公告列表 |
| 59 | PATCH | `/api/admin/announcements/{announcementId}` | 管理员 | 编辑公告 |
| 59 | DELETE | `/api/admin/announcements/{announcementId}` | 管理员 | 删除公告 |
| 60 | GET | `/api/announcements` | 公开 | 前台公告列表 |
