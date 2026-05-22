# P4 后端 Bug 修复日志

**项目：** CampusHub  
**阶段：** P4 编码开发  
**范围：** 已确认并完成修复的后端问题汇总

---

## 1. 记录目的

本日志用于记录 P4 阶段后端开发过程中已经确认并完成修复的缺陷，说明：

- 问题现象
- 影响范围
- 修复思路
- 涉及文件

---

## 2. 修复概览

本阶段目前已修复后端 Bug 共 **13** 项，主要集中在：

- 认证与验证码流程
- 通知与接口契约一致性
- 举报证据持久化
- 订单详情字段语义
- 文件上传后的归属与用途校验
- 静态资源访问权限
- 参数校验与错误处理
- 后台管理接口缺口与异常响应一致性
- 开发种子数据与联调文档一致性

---

## 3. 详细修复记录

### Bug 1：邮箱验证接口返回结构与前端不一致
**问题现象**

- 前端验证邮箱后，期望得到 `{ verified: true }`
- 后端原实现返回 `Void`
- 联调时前端无法按预期更新验证状态

**影响范围**

- 用户管理
- 邮箱验证流程

**修复方案**

- 新增专门的验证结果响应对象
- 将验证邮箱接口改为返回 `verified` 状态

**涉及文件**

- [AuthController.java](C:\Users\duoma\java\软工2项目\CampusHub\backend\src\main\java\com\campushub\controller\AuthController.java)
- [AuthService.java](C:\Users\duoma\java\软工2项目\CampusHub\backend\src\main\java\com\campushub\service\AuthService.java)
- [AuthServiceImpl.java](C:\Users\duoma\java\软工2项目\CampusHub\backend\src\main\java\com\campushub\service\impl\AuthServiceImpl.java)
- [VerifyEmailResponse.java](C:\Users\duoma\java\软工2项目\CampusHub\backend\src\main\java\com\campushub\dto\response\VerifyEmailResponse.java)

---

### Bug 2：举报证据上传后未与举报记录建立持久关联

**问题现象**

- 前端可以上传举报证据图片
- 举报提交时也能带 `evidenceImageIds`
- 但后端原来只把这些 ID 回显给前端，没有真正落库关联

**影响范围**

- 举报模块
- 文件上传模块

**修复方案**

- 新增 `report_evidence` 关联关系
- 举报提交成功后，将每一个证据文件与举报记录建立映射

**涉及文件**

- [ReportService.java](C:\Users\duoma\java\软工2项目\CampusHub\backend\src\main\java\com\campushub\service\ReportService.java)
- [ReportEvidence.java](C:\Users\duoma\java\软工2项目\CampusHub\backend\src\main\java\com\campushub\entity\ReportEvidence.java)
- [ReportEvidenceMapper.java](C:\Users\duoma\java\软工2项目\CampusHub\backend\src\main\java\com\campushub\mapper\ReportEvidenceMapper.java)
- [01-schema.sql](C:\Users\duoma\java\软工2项目\CampusHub\database\01-schema.sql)
- [03-reset-dev-data.sql](C:\Users\duoma\java\软工2项目\CampusHub\database\03-reset-dev-data.sql)

---

### Bug 3：订单详情中的 `completionNote` 错误返回为取消原因
**问题现象**

- 订单详情页展示 `completionNote`
- 后端原实现误把 `cancelReason` 填入 `completionNote`
- 导致完成备注与取消原因语义混淆

**影响范围**

- 订单管理
- 订单详情展示

**修复方案**

- 不再从订单实体的 `cancelReason` 取值
- 改为从状态日志中提取“提交完成”对应的备注信息

**涉及文件**

- [OrderService.java](C:\Users\duoma\java\软工2项目\CampusHub\backend\src\main\java\com\campushub\service\OrderService.java)

---

### Bug 4：任务配图、聊天图片、举报证据使用时缺少文件用途校验
**问题现象**

- 业务侧原来只根据文件 ID 查 `file_record`
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

- [FileService.java](C:\Users\duoma\java\软工2项目\CampusHub\backend\src\main\java\com\campushub\service\FileService.java)
- [TaskService.java](C:\Users\duoma\java\软工2项目\CampusHub\backend\src\main\java\com\campushub\service\TaskService.java)
- [OrderService.java](C:\Users\duoma\java\软工2项目\CampusHub\backend\src\main\java\com\campushub\service\OrderService.java)
- [ReportService.java](C:\Users\duoma\java\软工2项目\CampusHub\backend\src\main\java\com\campushub\service\ReportService.java)

---

### Bug 5：注册验证码发送逻辑错误地要求邮箱必须已存在

**问题现象**

- 注册前发送验证码时，后端原逻辑会检查邮箱是否已存在
- 新用户尚未注册，因此无法获取注册验证码

**影响范围**

- 用户管理
- 注册流程

**修复方案**

- 区分 `REGISTER` 与 `RESET_PASSWORD` 两种用途
- `REGISTER` 场景下：邮箱已存在才报错
- `RESET_PASSWORD` 场景下：邮箱不存在才报错

**涉及文件**

- [AuthServiceImpl.java](C:\Users\duoma\java\软工2项目\CampusHub\backend\src\main\java\com\campushub\service\impl\AuthServiceImpl.java)

---

### Bug 6：上传成功后的图片 URL 可能被安全策略拦截
**问题现象**

- 后端已经把 `/uploads/**` 映射到本地上传目录
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

- [SecurityConfig.java](C:\Users\duoma\java\软工2项目\CampusHub\backend\src\main\java\com\campushub\config\SecurityConfig.java)

---

### Bug 7：头像更新未经过 `file_record` 归属与用途校验
**问题现象**

- 更新资料时，后端原来直接信任前端传来的 `avatarUrl`
- 没有校验该头像文件是否属于当前用户、用途是否为头像

**影响范围**

- 用户管理
- 文件上传

**修复方案**

- 保持前端仍传 `avatarUrl`
- 后端改为根据 `fileUrl` 查询 `file_record`
- 校验：
  - 文件属于当前用户
  - 文件用途为 `AVATAR`
- 校验通过后再写入资料

**涉及文件**

- [UserServiceImpl.java](C:\Users\duoma\java\软工2项目\CampusHub\backend\src\main\java\com\campushub\service\impl\UserServiceImpl.java)
- [FileService.java](C:\Users\duoma\java\软工2项目\CampusHub\backend\src\main\java\com\campushub\service\FileService.java)

---

### Bug 8：完成凭证可复用任意自有上传文件，业务语义不严格

**问题现象**

- 原后端只校验完成凭证是否为当前用户自己的文件
- 没有区分这是不是“订单完成凭证专用文件”
- 理论上可能把头像、任务图、举报证据图拿来充当完成凭证

**影响范围**

- 订单管理
- 文件上传

**修复方案**

- 新增文件用途 `ORDER_PROOF`
- 完成订单时，完成凭证必须是：
  - 当前用户自己上传
  - 且用途为 `ORDER_PROOF`

**涉及文件**

- [UploadBusinessType.java](C:\Users\duoma\java\软工2项目\CampusHub\backend\src\main\java\com\campushub\enums\UploadBusinessType.java)
- [OrderService.java](C:\Users\duoma\java\软工2项目\CampusHub\backend\src\main\java\com\campushub\service\OrderService.java)

---

### Bug 9：注册接口未校验验证码，导致可绕过邮箱验证直接注册

**问题现象**

- 后端原注册逻辑只校验邮箱域名、密码一致性和邮箱是否已存在
- 没有校验 `REGISTER` 验证码是否存在、是否匹配、是否过期
- 任意人都可能绕过验证码直接创建账号

**影响范围**

- 用户管理
- 注册流程

**修复方案**

- 在注册请求中补充验证码字段
- 注册时查询最近一条未使用的 `REGISTER` 验证码
- 校验验证码是否存在、是否匹配、是否过期
- 校验通过后才允许创建用户
- 为兼容现有“注册后再 verify-email”的流程，注册时只校验，不在这里消耗验证码

**涉及文件**

- [RegisterRequest.java](C:\Users\duoma\java\软工2项目\CampusHub\backend\src\main\java\com\campushub\dto\request\RegisterRequest.java)
- [AuthServiceImpl.java](C:\Users\duoma\java\软工2项目\CampusHub\backend\src\main\java\com\campushub\service\impl\AuthServiceImpl.java)

---

### Bug 10：任务大厅非法分类参数会直接变成 500

**问题现象**

- 任务列表接口原来直接执行 `TaskCategory.valueOf(category)`
- 当前端传入非法分类值时，会抛 `IllegalArgumentException`
- 异常最终被兜底成 500，而不是友好的参数错误提示

**影响范围**

- 任务大厅
- 需求筛选

**修复方案**

- 将分类解析改为安全解析
- 非法分类值时主动抛出 `BusinessException(ErrorCode.BAD_REQUEST, ...)`
- 返回明确的业务错误，而不是服务器内部错误

**涉及文件**

- [TaskService.java](C:\Users\duoma\java\软工2项目\CampusHub\backend\src\main\java\com\campushub\service\TaskService.java)

---

### Bug 11：更新资料接口缺少 `@Valid`，参数长度约束未真正生效

**问题现象**

- `UpdateProfileRequest` 中已经写了 `@Size`
- 但 `UserController` 的更新资料接口没有加 `@Valid`
- 导致昵称、简介长度等约束写了但不会被 Spring 真正校验

**影响范围**

- 用户管理
- 个人资料更新

**修复方案**

- 在更新资料接口的 `@RequestBody` 参数前补充 `@Valid`
- 让 `UpdateProfileRequest` 中的校验注解真正生效

**涉及文件**

- [UserController.java](C:\Users\duoma\java\软工2项目\CampusHub\backend\src\main\java\com\campushub\controller\UserController.java)

---

### Bug 12：管理员用户列表接口未实现，真实后端查询返回错误

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
