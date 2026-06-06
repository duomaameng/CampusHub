# P4-05 CI/CD 配置与运行记录

**项目：** CampusHub  
**阶段：** P4 编码开发  
**对应交付物：** 5. CI/CD 配置与运行记录  

---

## 1. 文档目的

本文档记录 P4 阶段的 CI/CD 配置。由于本阶段新增了后端服务层单元测试和前端 Vitest 单元测试，CI 配置也同步更新，确保提交后能自动执行这些防回归测试。

---

## 2. CI/CD 配置文件

| 文件 | 作用 |
|------|------|
| `.gitlab-ci.yml` | 顶层流水线，根据变更路径触发前端或后端子流水线 |
| `.gitlab/backend.yml` | 后端编译、单元测试、集成测试 |
| `.gitlab/frontend.yml` | 前端依赖安装、单元测试、类型检查和构建 |

---

## 3. 后端流水线

后端配置文件：

```text
.gitlab/backend.yml
```

后端阶段：

```text
build -> test
```

| 任务 | 命令 | 作用 |
|------|------|------|
| `backend-build` | `mvn -B -q clean compile` | 验证后端可以编译 |
| `backend-unit-test` | `mvn -B -q -Dtest=AdminServiceTest,OrderServiceTest,AuthServiceImplTest,EmailServiceTest,FileServiceTest,NotificationFactoryTest,NotificationServiceTest,ReportServiceTest,TaskServiceTest,UserServiceImplTest test` | 运行后端服务层单元测试 |
| `backend-integration-test` | `mvn -B -q -Dtest=CoreFlowIntegrationTest test` | 运行核心流程集成测试 |

后端测试报告产物：

```text
backend/target/surefire-reports
```

---

## 4. 前端流水线

前端配置文件：

```text
.gitlab/frontend.yml
```

前端任务：

| 任务 | 命令 | 作用 |
|------|------|------|
| `frontend-build` | `npm ci` | 按锁文件安装依赖 |
| `frontend-build` | `npm run test` | 运行 Vitest 单元测试 |
| `frontend-build` | `npm run build` | 执行 Vue 类型检查和 Vite 生产构建 |

前端构建产物：

```text
frontend/dist
```

---

## 5. 本地等价命令

后端：

```bash
cd backend
mvn clean compile
mvn -Dtest=AdminServiceTest,OrderServiceTest,AuthServiceImplTest,EmailServiceTest,FileServiceTest,NotificationFactoryTest,NotificationServiceTest,ReportServiceTest,TaskServiceTest,UserServiceImplTest test
mvn -Dtest=CoreFlowIntegrationTest test
```

前端：

```bash
cd frontend
npm ci
npm run test
npm run build
```

---

## 6. 本地运行记录

| 检查项 | 结果 |
|------|------|
| 后端服务层单元测试 | 已通过 |
| 前端 Vitest 单元测试 | 已通过，2 个测试文件、3 个用例 |
| 前端 lock 文件 | 已通过 `npm install` 同步 |
| CI 后端单测命令 | 已更新为包含新增测试类 |
| CI 前端命令 | 已新增 `npm run test` |

说明：GitLab 页面中的具体流水线编号、截图和运行耗时，需要以提交到 GitLab 后的实际流水线页面为准。

---

## 7. 当前不足

1. 后端尚未接入 JaCoCo 覆盖率统计。
2. 前端目前只覆盖工具函数级别的防回归测试，尚未覆盖 Vue 组件交互。
3. 流水线尚未配置自动部署。
4. 数据库脚本变化目前没有单独触发后端测试。

后续可继续补充覆盖率报告、前端组件测试、数据库脚本校验和演示环境部署任务。

