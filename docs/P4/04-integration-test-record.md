# P4-04 集成测试代码与运行记录

**项目：** CampusHub  
**阶段：** P4 编码开发  
**对应交付物：** 4. 集成测试代码  

---

## 1. 文档目的

本文档用于记录 P4 阶段集成测试代码、覆盖流程、运行方式和验收结果。

集成测试关注多个模块之间的真实协作关系，重点验证认证、需求发布、接单申请、确认接单、订单生成等核心链路是否能够在 Spring Boot 应用上下文中贯通。

---

## 2. 集成测试文件清单

| 文件 | 类型 | 说明 |
|------|------|------|
| `backend/src/test/java/com/campushub/integration/CoreFlowIntegrationTest.java` | 后端集成测试 | 使用 Spring Boot Test、MockMvc、H2 测试数据库和测试 Schema 验证核心业务链路 |
| `backend/src/test/resources/application-test.yml` | 测试配置 | 提供测试环境配置 |
| `backend/src/test/resources/schema-test.sql` | 测试数据库 Schema | 每个测试方法执行前重建测试表结构 |

---

## 3. 测试环境

| 项目 | 内容 |
|------|------|
| 测试框架 | Spring Boot Test + JUnit 5 |
| HTTP 调用模拟 | MockMvc |
| 数据库 | H2 测试数据库 |
| 测试 Profile | `test` |
| 数据初始化 | `@Sql(scripts = "classpath:schema-test.sql", executionPhase = BEFORE_TEST_METHOD)` |

集成测试不会依赖本地 MySQL 中的真实业务数据，而是在每个测试方法前使用测试 Schema 重建数据环境，保证测试之间互不污染。

---

## 4. 已覆盖的正常流程

### 4.1 登录流程

测试方法：

```text
shouldLoginSuccessfullyAndReturnJwtToken
```

覆盖内容：

1. 创建已验证学生用户。
2. 调用 `POST /api/auth/login`。
3. 验证响应状态为成功。
4. 验证返回 JWT Token。
5. 验证返回用户邮箱与登录账号一致。

### 4.2 发布需求 -> 申请接单 -> 确认接单 -> 生成订单

测试方法：

```text
shouldCreateOrderThroughPublishApplyConfirmFlow
```

覆盖内容：

1. 创建发布方和服务方两个用户。
2. 两个用户分别登录并获取 Token。
3. 发布方调用 `POST /api/tasks` 发布需求。
4. 服务方调用 `POST /api/tasks/{taskId}/applications` 申请接单。
5. 发布方调用 `POST /api/applications/{applicationId}/confirm` 确认接单。
6. 验证任务状态变为 `IN_PROGRESS`。
7. 验证申请状态变为 `APPROVED`。
8. 验证订单创建成功且状态为 `IN_PROGRESS`。

该流程覆盖 P4 阶段最核心的业务链路。

---

## 5. 已覆盖的异常流程

| 测试方法 | 异常场景 | 预期结果 |
|----------|----------|----------|
| `shouldRejectTaskCreationWhenUnauthenticated` | 未登录用户发布需求 | 请求被拒绝，数据库不新增任务 |
| `shouldRejectProtectedRequestAfterLogoutWithSameToken` | 用户登出后继续使用旧 Token 访问受保护接口 | 请求被拒绝 |
| `shouldRejectDuplicateApplicationConfirmation` | 同一个接单申请重复确认 | 第二次确认返回业务错误，订单数量仍为 1 |

这些异常流程分别覆盖未登录访问、Token 失效和重复成单三类高风险问题。

---

## 6. 运行方式

在项目根目录执行：

```bash
cd backend
mvn -Dtest=CoreFlowIntegrationTest test
```

GitLab CI 中后端集成测试任务使用以下命令：

```bash
cd backend
mvn -B -q -Dtest=CoreFlowIntegrationTest test
```

对应 CI 任务：

```text
backend-integration-test
```

---

## 7. 测试结果记录

| 检查项 | 记录 |
|------|------|
| 正常流程数量 | 2 个主要正常流程 |
| 异常流程数量 | 3 个异常流程 |
| 覆盖接口 | 登录、登出、发布需求、申请接单、确认接单、当前用户信息 |
| 覆盖模块 | 认证、任务、接单申请、订单 |
| 测试报告产物 | `backend/target/surefire-reports` |

---

## 8. 当前不足与改进计划

当前集成测试已经覆盖主链路前半段，即“登录 -> 发布 -> 申请 -> 确认 -> 成单”。P4 后续仍可继续补充：

1. 订单完成与确认完成的 HTTP 集成测试。
2. 评价提交后的信用流水集成测试。
3. 举报提交和管理员处理举报的集成测试。
4. 文件上传接口的集成测试。
5. 前端端到端自动化测试。

本阶段优先保证核心成单链路和关键异常路径可自动验证。
