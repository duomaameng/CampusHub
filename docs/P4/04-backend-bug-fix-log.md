# P4 后端 Bug 修复日志

**项目：** CampusHub  
**阶段：** P4 编码开发
**范围：** 已修复问题汇总

---

## 1. 记录目的

本日志用于记录 P4 阶段后端开发过程中已经确认并完成修复的缺陷，说明：

- 问题现象
- 影响范围
- 修复思路
- 涉及文件

---

## 2. 修复概览

本阶段已修复后端 Bug 共 **8** 项，主要集中在：

- 认证与验证码流程
- 通知与接口契约一致性
- 举报证据持久化
- 订单详情字段语义
- 文件上传后的归属与用途校验
- 静态资源访问权限

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
- 举报提交成功后，将每个证据文件与举报记录建立映射

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

### Bug 5：注册验证码流程错误地要求邮箱必须已存在

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

