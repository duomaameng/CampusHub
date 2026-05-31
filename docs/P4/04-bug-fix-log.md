# P4 后端 Bug 修复日志

**项目：** CampusHub  
**阶段：** P4 编码开发  
**范围：** 已确认并完成修复的后端问题记录

---

## 1. 记录目的

本日志用于记录 P4 阶段后端开发过程中已经确认并修复的问题，主要说明：

- 问题现象
- 影响范围
- 修复思路
- 涉及文件

---

## 2. 修复概览

本阶段目前已修复 Bug 共 **20** 项，主要集中在：

- 认证与验证码流程
- 任务、订单、举报主链
- 文件上传与文件归属校验
- 静态资源访问权限
- 参数校验与错误处理
- 后台管理接口缺口与异常响应一致性
- 开发种子数据与联调文档一致性
- 前端注册页与后端验证码契约一致性
- SMTP 验证码邮件发送能力
- 前后端分离部署下上传图片资源地址解析
- 用户资料与真实信用数据一致性
- 接单确认并发保护
- 后台订单冻结/恢复接口契约一致性

---

## 3. 详细修复记录

### Bug 1：邮箱验证接口返回结构与前端不一致

**问题现象**

- 前端验证邮箱后，期望得到 `{ verified: true }`
- 后端原实现返回 `Void`

**影响范围**

- 用户管理
- 邮箱验证流程

**修复方案**

- 新增专用响应对象
- 将验证邮箱接口改为返回 `verified` 状态

**涉及文件**

- `backend/src/main/java/com/campushub/controller/AuthController.java`
- `backend/src/main/java/com/campushub/service/AuthService.java`
- `backend/src/main/java/com/campushub/service/impl/AuthServiceImpl.java`
- `backend/src/main/java/com/campushub/dto/response/VerifyEmailResponse.java`

---

### Bug 2：举报证据上传后未与举报记录建立持久关联

**问题现象**

- 前端可以上传举报证据图片
- 举报提交时可以携带 `evidenceImageIds`
- 后端原实现只回显这些 ID，没有真正落库关联

**影响范围**

- 举报模块
- 文件上传模块

**修复方案**

- 新增 `report_evidence` 关联表
- 举报提交成功后，将证据文件与举报记录建立映射关系

**涉及文件**

- `backend/src/main/java/com/campushub/service/ReportService.java`
- `backend/src/main/java/com/campushub/entity/ReportEvidence.java`
- `backend/src/main/java/com/campushub/mapper/ReportEvidenceMapper.java`
- `database/01-schema.sql`
- `database/03-reset-dev-data.sql`

---

### Bug 3：订单详情中的 `completionNote` 错误返回为取消原因

**问题现象**

- 订单详情页面展示 `completionNote`
- 后端原实现错误地把 `cancelReason` 填入了该字段

**影响范围**

- 订单管理
- 订单详情展示

**修复方案**

- 不再从订单实体的 `cancelReason` 取值
- 改为从状态日志中提取“提交完成”对应的备注

**涉及文件**

- `backend/src/main/java/com/campushub/service/OrderService.java`

---

### Bug 4：任务配图、聊天图片、举报证据使用时缺少用途校验

**问题现象**

- 业务侧原来只按文件 ID 查询 `file_record`
- 没有严格校验文件用途是否匹配当前业务

**影响范围**

- 任务发布
- 订单消息
- 举报模块

**修复方案**

- 在文件服务中补充“文件归属 + 文件用途”双重校验
- 任务配图只能使用 `TASK_IMAGE`
- 聊天图片只能使用 `CHAT_IMAGE`
- 举报证据只能使用 `REPORT_EVIDENCE`

**涉及文件**

- `backend/src/main/java/com/campushub/service/FileService.java`
- `backend/src/main/java/com/campushub/service/TaskService.java`
- `backend/src/main/java/com/campushub/service/OrderService.java`
- `backend/src/main/java/com/campushub/service/ReportService.java`

---

### Bug 5：注册验证码发送逻辑错误要求邮箱必须已存在

**问题现象**

- 注册前发送验证码时，后端原逻辑会检查邮箱是否已存在
- 新用户尚未注册，因此无法获取注册验证码

**影响范围**

- 用户管理
- 注册流程

**修复方案**

- 区分 `REGISTER` 与 `RESET_PASSWORD`
- `REGISTER` 场景下：邮箱已存在才报错
- `RESET_PASSWORD` 场景下：邮箱不存在才报错

**涉及文件**

- `backend/src/main/java/com/campushub/service/impl/AuthServiceImpl.java`

---

### Bug 6：上传成功后的图片 URL 可能被安全策略拦截

**问题现象**

- `/uploads/**` 已映射到本地目录
- 但安全配置未放行该路径
- 浏览器访问上传图片时可能得到 401/403

**影响范围**

- 头像显示
- 任务配图显示
- 聊天图片显示
- 举报证据显示

**修复方案**

- 在安全配置中将 `/uploads/**` 加入白名单

**涉及文件**

- `backend/src/main/java/com/campushub/config/SecurityConfig.java`

---

### Bug 7：头像更新未经过 `file_record` 归属与用途校验

**问题现象**

- 更新资料时，后端原来直接信任前端传来的 `avatarUrl`
- 没有验证该文件是否属于当前用户，且用途是否为头像

**影响范围**

- 用户管理
- 文件上传

**修复方案**

- 保持前端仍传 `avatarUrl`
- 后端根据 `fileUrl` 反查 `file_record`
- 校验文件归属当前用户，且用途为 `AVATAR`

**涉及文件**

- `backend/src/main/java/com/campushub/service/impl/UserServiceImpl.java`
- `backend/src/main/java/com/campushub/service/FileService.java`

---

### Bug 8：完成凭证可复用任意自有上传文件，业务语义不严谨

**问题现象**

- 原后端只校验完成凭证是否为当前用户自己的文件
- 没有区分这是否是“订单完成凭证专用文件”

**影响范围**

- 订单管理
- 文件上传

**修复方案**

- 新增文件用途 `ORDER_PROOF`
- 完成订单时，完成凭证必须属于当前用户，且用途为 `ORDER_PROOF`

**涉及文件**

- `backend/src/main/java/com/campushub/enums/UploadBusinessType.java`
- `backend/src/main/java/com/campushub/service/OrderService.java`

---

### Bug 9：注册接口未校验验证码，导致可绕过邮箱验证直接注册

**问题现象**

- 原注册逻辑只校验邮箱后缀、密码一致性和邮箱是否已存在
- 没有校验 `REGISTER` 验证码是否存在、匹配、过期

**影响范围**

- 用户管理
- 注册流程

**修复方案**

- 在注册请求中补充验证码字段
- 注册时查询最近一条未使用的 `REGISTER` 验证码
- 校验存在性、匹配性和过期状态

**涉及文件**

- `backend/src/main/java/com/campushub/dto/request/RegisterRequest.java`
- `backend/src/main/java/com/campushub/service/impl/AuthServiceImpl.java`

---

### Bug 10：任务大厅非法分类参数会直接变成 500

**问题现象**

- 任务列表接口原来直接执行 `TaskCategory.valueOf(category)`
- 非法分类值会抛 `IllegalArgumentException`
- 最终被兜底成 500，而不是友好的参数错误

**影响范围**

- 任务大厅
- 需求筛选

**修复方案**

- 改为安全解析分类参数
- 非法值时主动抛出 `BusinessException(ErrorCode.BAD_REQUEST, ...)`

**涉及文件**

- `backend/src/main/java/com/campushub/service/TaskService.java`

---

### Bug 11：更新资料接口缺少 `@Valid`，参数约束未真正生效

**问题现象**

- `UpdateProfileRequest` 中已经写了 `@Size`
- 但 `UserController` 更新资料接口未加 `@Valid`

**影响范围**

- 用户管理
- 个人资料更新

**修复方案**

- 在更新资料接口参数上补充 `@Valid`

**涉及文件**

- `backend/src/main/java/com/campushub/controller/UserController.java`

---

### Bug 12：数据库连接编码参数错误导致真实联调登录时报 500

**问题现象**

- 前端切换到真实后端后，登录接口返回 500
- 后端控制台报错：`Unsupported character encoding 'utf8mb4'`

**影响范围**

- 前后端联调
- 登录接口
- 所有依赖数据库的接口

**修复方案**

- 将 JDBC URL 中的 `characterEncoding=utf8mb4` 改为 `characterEncoding=UTF-8`

**涉及文件**

- `backend/src/main/resources/application.yml`

---

### Bug 13：管理员用户列表接口未实现，真实后端查询返回错误

**问题现象**

- 前端管理员后台页面在加载时会请求 `GET /api/admin/users?page=1&size=20`
- Mock 环境中该接口存在，但真实后端没有实现 `/api/admin/users`
- 管理员登录后进入用户管理页时，请求会落到不存在的路径，联调时表现为查询失败
- 部分未匹配路径、非法枚举或错误请求体也可能被全局兜底成 500，不符合文档中 404/400 的错误语义

**影响范围**

- 后台管理
- 用户管理
- 前后端接口联调
- 统一异常处理

**修复方案**

- 新增 `AdminController`，补齐：
  - `GET /api/admin/users`
  - `PATCH /api/admin/users/{userId}/status`
- 新增 `AdminService`，保持 Controller 不直接访问数据库，符合分层约束
- 用户列表支持分页、关键字搜索、状态筛选和认证状态筛选
- 分页默认由前端传入 `page=1&size=20`，后端限制 `size` 最大为 50
- 管理员更新用户状态时只允许 `ACTIVE` / `DISABLED`
- 状态更新后写入 `admin_operation_log`，满足管理员操作审计要求
- 补充异常处理：
  - 未匹配路径返回 HTTP 404 和统一 `NOT_FOUND` 响应
  - 非法枚举、错误 JSON、缺少必要参数返回 HTTP 400 和统一 `BAD_REQUEST` 响应
- 新增 `AdminServiceTest` 覆盖用户列表、状态更新、审计日志和非法状态拒绝

**涉及文件**

- [AdminController.java](C:\Users\duoma\java\软工2项目\CampusHub\backend\src\main\java\com\campushub\controller\AdminController.java)
- [AdminService.java](C:\Users\duoma\java\软工2项目\CampusHub\backend\src\main\java\com\campushub\service\AdminService.java)
- [AdminUserStatusRequest.java](C:\Users\duoma\java\软工2项目\CampusHub\backend\src\main\java\com\campushub\dto\admin\AdminUserStatusRequest.java)
- [AdminUserItemVO.java](C:\Users\duoma\java\软工2项目\CampusHub\backend\src\main\java\com\campushub\vo\admin\AdminUserItemVO.java)
- [AdminUserStatusVO.java](C:\Users\duoma\java\软工2项目\CampusHub\backend\src\main\java\com\campushub\vo\admin\AdminUserStatusVO.java)
- [GlobalExceptionHandler.java](C:\Users\duoma\java\软工2项目\CampusHub\backend\src\main\java\com\campushub\exception\GlobalExceptionHandler.java)
- [AdminServiceTest.java](C:\Users\duoma\java\软工2项目\CampusHub\backend\src\test\java\com\campushub\service\AdminServiceTest.java)

---

### Bug 13：README 默认测试密码与种子数据中的密码哈希不匹配

**问题现象**

- `database/README.md` 中说明默认测试账号共享密码为 `Password123!`
- 但 `database/02-seed.sql` 中插入的 `password_hash` 并不匹配 `Password123!`
- 前后端联调时，按照 README 使用管理员账号 `admin@smail.nju.edu.cn / Password123!` 登录会失败
- 该问题容易被误判为前端登录请求、后端鉴权或数据库连接错误，实际根因是文档与种子数据不一致

**影响范围**

- 数据库初始化
- 登录联调
- 管理员后台访问
- 测试账号说明文档

**修复方案**

- 使用后端同款 `BCryptPasswordEncoder` 校验原种子哈希，确认其不匹配 `Password123!`
- 重新生成匹配 `Password123!` 的 bcrypt 哈希
- 将 `02-seed.sql` 中 4 个测试账号的 `password_hash` 统一更新为匹配 `Password123!` 的哈希
- 将 `database/README.md` 中默认账号说明改为账号、角色、密码对应表
- 明确提醒：如果本地数据库已经导入过旧 seed，需要重新执行 reset + seed，或手动更新数据库中的 `user.password_hash`

**涉及文件**

- [02-seed.sql](C:\Users\duoma\java\软工2项目\CampusHub\database\02-seed.sql)
- [README.md](C:\Users\duoma\java\软工2项目\CampusHub\database\README.md)

---

### Bug 14：前端注册页缺少验证码流程，导致真实后端注册失败

**问题现象**

- 后端 `RegisterRequest` 已要求注册请求必须携带 `code`
- 后端注册服务会校验最近一条未使用的 `REGISTER` 验证码
- 前端注册页原来没有发送验证码入口，也没有验证码输入框
- 前端 `authApi.register` 只提交 `email`、`password`、`confirmPassword`，真实后端会因缺少验证码拒绝注册
- mock 注册发码逻辑仍要求邮箱已存在，与真实后端的 `REGISTER` 场景规则不一致

**影响范围**

- 用户注册
- 邮箱验证码流程
- 前后端联调

**修复方案**

- 注册页新增发送 `REGISTER` 验证码按钮
- 注册页新增 6 位验证码输入框
- 前端注册请求补充 `code` 字段，与后端 `RegisterRequest` 保持一致
- Pinia auth store 同步更新注册方法签名
- mock API 同步后端规则：
  - `REGISTER` 发码允许未注册邮箱，已注册邮箱报错
  - `RESET_PASSWORD` 发码仍要求邮箱已注册
  - mock 注册时校验验证码是否存在、是否匹配、是否过期
  - mock 邮箱验证时同步校验验证码过期时间

**涉及文件**

- [RegisterView.vue](C:\Users\duoma\java\软工2项目\CampusHub\frontend\src\views\RegisterView.vue)
- [api.ts](C:\Users\duoma\java\软工2项目\CampusHub\frontend\src\services\api.ts)
- [auth.ts](C:\Users\duoma\java\软工2项目\CampusHub\frontend\src\stores\auth.ts)
- [mock.ts](C:\Users\duoma\java\soft工2项目\CampusHub\frontend\src\services\mock.ts)

---

### Bug 15：真实后端验证码只写入数据库和日志，没有实际发送邮件

**问题现象**

- 后端已经配置了 `spring.mail`
- 项目也引入了 `spring-boot-starter-mail`
- 但 `sendVerificationCode` 原实现只生成验证码、写入 `verification_code` 表，并把验证码打印到日志
- 本地真实后端联调时，用户点击发送验证码后不会收到邮件
- 前端真实后端模式下仍提示 mock 固定验证码，容易误导联调判断

**影响范围**

- 用户注册
- 邮箱验证
- 忘记密码与密码重置
- 前后端真实后端联调

**修复方案**

- 新增 `EmailService`，统一负责验证码投递
- 支持通过 `MAIL_DELIVERY_MODE` 切换投递方式：
  - `smtp`：使用 `JavaMailSender` 真实发送验证码邮件
  - `log`：仅在后端日志打印验证码，方便无 SMTP 服务的本地/临时环境
- `sendVerificationCode` 改为验证码入库后调用邮件服务
- 邮件发送失败时返回明确的业务错误，避免前端误显示“已发送”
- 发送验证码接口加事务，邮件失败时回滚本次验证码记录
- 新增 `EMAIL_SEND_FAILED` 错误码
- 前端验证码提示根据 mock/真实后端模式区分文案
- README 补充 `MAIL_DELIVERY_MODE`、`MAIL_PORT`、`MAIL_FROM`、本地个人邮箱和服务器邮箱配置说明

**涉及文件**

- [EmailService.java](C:\Users\duoma\java\软工2项目\CampusHub\backend\src\main\java\com\campushub\service\EmailService.java)
- [AuthServiceImpl.java](C:\Users\duoma\java\软工2项目\CampusHub\backend\src\main\java\com\campushub\service\impl\AuthServiceImpl.java)
- [ErrorCode.java](C:\Users\duoma\java\软工2项目\CampusHub\backend\src\main\java\com\campushub\common\ErrorCode.java)
- [application.yml](C:\Users\duoma\java\软工2项目\CampusHub\backend\src\main\resources\application.yml)
- [RegisterView.vue](C:\Users\duoma\java\软工2项目\CampusHub\frontend\src\views\RegisterView.vue)
- [ForgotPasswordView.vue](C:\Users\duoma\java\软工2项目\CampusHub\frontend\src\views\ForgotPasswordView.vue)
- [VerifyEmailView.vue](C:\Users\duoma\java\软工2项目\CampusHub\frontend\src\views\VerifyEmailView.vue)
- [README.md](C:\Users\duoma\java\软工2项目\CampusHub\README.md)

---

### Bug 16：前后端分离部署时上传图片使用相对路径导致前端无法显示

**问题现象**

- 后端上传接口保存图片成功，并返回 `/uploads/xxx.png`
- 前端在任务配图、头像、聊天图片和举报证据预览中直接使用该相对路径作为 `<img src>`
- 本地联调时前端运行在 `http://localhost:5173`，后端运行在 `http://localhost:8080`
- 浏览器会把 `/uploads/xxx.png` 请求到前端站点，实际文件却由后端 `/uploads/**` 暴露，因此图片显示失败

**影响范围**

- 任务发布配图预览
- 任务详情配图展示
- 个人头像预览
- 订单聊天图片预览与展示
- 举报证据图片预览
- 前后端分离部署后的上传图片访问

**修复方案**

- 前端新增统一资源地址解析函数 `resolveAssetUrl`
- 优先读取 `VITE_ASSET_BASE_URL`，将 `/uploads/xxx.png` 补全为后端资源域名下的完整地址
- 若未显式配置 `VITE_ASSET_BASE_URL`，则尝试从 `VITE_API_BASE_URL` 推导后端根地址
- 对 `http://`、`https://`、`blob:`、`data:` 等已可直接访问的地址保持原样
- 所有上传图片展示点统一调用 `resolveAssetUrl`
- README 补充本地联调与部署时的资源地址配置说明

**涉及文件**

- `frontend/src/utils/assets.ts`
- `frontend/src/env.d.ts`
- `frontend/src/views/TaskPublishView.vue`
- `frontend/src/views/TaskDetailView.vue`
- `frontend/src/views/ProfileView.vue`
- `frontend/src/views/OrderDetailView.vue`
- `frontend/.env.local`
- `README.md`

---

### Bug 17：ReportController 与 AdminController 存在重复 URL 映射导致 ApplicationContext 启动失败

**问题现象**

- Spring Boot 启动时抛出 `Ambiguous mapping` 异常
- `ReportController.listAdminReports()` 映射到 `GET /api/admin/reports`
- `AdminController.reports()` 也已映射到 `GET /api/admin/reports`（AdminController 使用 `@RequestMapping("/api/admin")`）
- 同样 `ReportController.process()` 映射到 `PATCH /api/admin/reports/{reportId}` 冲突

**影响范围**

- 后端启动
- 后台管理举报模块

**修复方案**

- 将 `GET /api/admin/reports` 和 `PATCH /api/admin/reports/{reportId}` 统一收归 `AdminController`
- 从 `ReportController` 移除 `listAdminReports` 和 `process` 两个方法
- `AdminController` 注入 `ReportService`，新增 `PATCH /reports/{reportId}` 端点以覆盖 AD-10 处理举报
- `AdminController.reports()` 保持不变（AD-09 举报管理列表）

**涉及文件**

- `backend/src/main/java/com/campushub/controller/AdminController.java`
- `backend/src/main/java/com/campushub/controller/ReportController.java`

---

### Bug 18：用户资料接口返回的信用数据是写死值

**问题现象**

- `GET /api/users/me`
- `PATCH /api/users/me`
- `GET /api/users/{userId}/profile`

这些接口返回的信用摘要原先固定为：

- `creditScore = 100`
- `completedOrders = 0`
- `praiseRate = 1.0`

即使用户已有真实信用变动、完成订单和评价记录，资料接口仍展示假数据。

**影响范围**

- 当前用户资料页
- 公开用户资料页
- 更新资料后的即时回显

**修复方案**

- 在 `UserServiceImpl` 中抽出统一的信用摘要计算方法
- 复用真实的：
  - 信用日志最新分数
  - 已完成订单数
  - 平均评分换算出的好评率
- 让资料页与信用接口口径保持一致

**涉及文件**

- `backend/src/main/java/com/campushub/service/impl/UserServiceImpl.java`

---

### Bug 19：确认接单流程缺少并发保护，可能重复成单

**问题现象**

- `POST /api/applications/{applicationId}/confirm`

原逻辑先检查任务状态是否为 `OPEN`，随后再更新任务状态并创建订单。  
在两个确认请求几乎同时进入时，存在并发下重复创建订单的风险。

**影响范围**

- 接单确认主链
- 订单唯一性

**修复方案**

- 在 `TaskMapper` 中增加带条件的状态更新：
  - 只有当前状态仍为 `OPEN` 时，才允许改成 `IN_PROGRESS`
- `TaskService.confirmApplication(...)` 依据更新影响行数判断是否抢占成功
- 若条件更新失败，则视为任务已被他人确认接单，直接返回业务错误

**涉及文件**

- `backend/src/main/java/com/campushub/mapper/TaskMapper.java`
- `backend/src/main/java/com/campushub/service/TaskService.java`

---

### Bug 20：后台订单“恢复”接口请求语义与实际结果不一致

**问题现象**

- `PATCH /api/admin/orders/{orderId}/status`

上一版虽然已经收紧到“冻结 / 恢复”语义，但恢复请求仍要求传 `IN_PROGRESS`，  
而真实恢复结果可能是：

- `PENDING_CONFIRM`
- `PENDING_COMPLETION`
- `IN_PROGRESS`

这会导致：

- 请求里写的是 `IN_PROGRESS`
- 最终结果却不一定是 `IN_PROGRESS`

前后端接口契约存在误导。

**影响范围**

- 后台订单管理
- 前后端联调

**修复方案**

- 仍保留 `DISPUTE` 表示冻结
- 当订单当前处于 `DISPUTE` 时，恢复请求必须显式传入“冻结前原状态”
- 后端根据管理员操作日志取出冻结前状态，并校验请求值必须与其一致
- 只有一致时才允许恢复，确保“请求状态 = 最终状态”

**涉及文件**

- `backend/src/main/java/com/campushub/service/AdminService.java`
- `backend/src/main/java/com/campushub/dto/admin/AdminOrderStatusUpdateRequest.java`

---

### Bug 21：任务配图上传成功后前端预览图片不显示

**问题现象**

- 用户在发布需求页面上传任务配图后，上传接口已经返回成功，上传目录中也能看到实际图片文件。
- 但前端预览卡片中的 `<img>` 显示为破图，只能看到文件名，无法看到图片内容。
- 重启前端开发服务器后问题仍然存在。

**影响范围**

- 发布需求页面的任务配图上传预览。
- 后续依赖上传图片回显的任务详情、订单留言图片等展示场景。

**修复方案**

- 为 Vite 开发环境补充 `/uploads` 代理，将前端站点下的 `/uploads/**` 请求转发到真实后端服务。
- 调整后端静态资源映射，确保本地上传目录以规范的绝对路径暴露给 `/uploads/**`。
- 保持图片地址仍使用后端返回的 `/uploads/...` 路径，避免前端各页面重复拼接资源域名。

**涉及文件**

- `frontend/vite.config.ts`
- `backend/src/main/java/com/campushub/config/WebMvcConfig.java`

---

### Bug 22：任务配图点击移除后只从前端隐藏，服务器文件未真实删除

**问题现象**

- 用户上传任务配图后点击“移除”，前端预览卡片会消失。
- 但打开本地 `uploads` 目录后，刚才上传的图片文件仍然存在。
- 多次上传和移除会在服务器目录中遗留无效文件，造成存储污染。
- 初次补充删除请求时，前端出现 `Request failed with status code 404`，说明前后端删除接口契约没有闭环。

**影响范围**

- 发布需求页面的任务配图管理。
- 文件上传模块的资源清理。
- 后续长期使用时的无效文件堆积问题。

**修复方案**

- 后端新增文件删除接口 `DELETE /api/files/{fileId}`。
- 删除前校验文件归属，确保用户只能删除自己上传的文件。
- 后端删除数据库中的 `file_record` 记录，并同步删除磁盘上的实际上传文件。
- 前端 `fileApi` 新增删除方法，点击“移除”时先调用后端删除接口，成功后再从页面列表移除。
- 对删除失败场景保留错误提示，避免用户误以为文件已经清理。

**涉及文件**

- `backend/src/main/java/com/campushub/controller/FileController.java`
- `backend/src/main/java/com/campushub/service/FileService.java`
- `frontend/src/services/api.ts`
- `frontend/src/services/mock.ts`
- `frontend/src/views/TaskPublishView.vue`

---

### Bug 23：服务方提交完成时前端误传写死的完成凭证文件 ID

**问题现象**

- 服务方在订单详情页点击“提交完成”后，页面顶部报错：`You can only use files uploaded by yourself`。
- 实际用户并没有在提交完成时选择完成凭证图片。
- 前端请求体中写死传入了 `proofImageId: 1`。
- 后端会按完成凭证规则校验该文件是否属于当前用户且用途正确，因此当文件 1 不属于当前服务方时会被拒绝。

**影响范围**

- 订单详情页服务方提交完成流程。
- 订单状态从 `IN_PROGRESS` 流转到 `PENDING_COMPLETION` 的主链路。

**修复方案**

- 移除前端提交完成接口中的硬编码 `proofImageId: 1`。
- 在未提供完成凭证上传入口的情况下，只提交完成说明 `note`。
- 保留后端对 `proofImageId` 的归属与用途校验，确保未来补充完成凭证上传时仍然安全。

**涉及文件**

- `frontend/src/services/api.ts`
- `backend/src/main/java/com/campushub/service/OrderService.java`

---
