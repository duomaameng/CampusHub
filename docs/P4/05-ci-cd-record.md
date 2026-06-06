# P4-05 CI/CD 配置与运行记录

**项目：** CampusHub  
**阶段：** P4 编码开发  
**对应交付物：** 5. CI/CD 配置与运行记录  

---

## 1. 文档目的

本文档用于说明 CampusHub 在 P4 阶段配置的 CI/CD 流程，包括流水线结构、前后端任务、自动检查内容、产物保存方式和当前运行记录。

---

## 2. CI/CD 配置文件

| 文件 | 作用 |
|------|------|
| `.gitlab-ci.yml` | 顶层流水线配置，根据变更路径触发前端或后端子流水线 |
| `.gitlab/backend.yml` | 后端构建、单元测试、集成测试配置 |
| `.gitlab/frontend.yml` | 前端依赖安装、类型检查和构建配置 |

---

## 3. 顶层流水线结构

`.gitlab-ci.yml` 使用 trigger 方式拆分前后端流水线：

| 任务 | 触发条件 | 子配置 |
|------|----------|--------|
| `backend` | `backend/**/*` 发生变化 | `.gitlab/backend.yml` |
| `frontend` | `frontend/**/*` 发生变化 | `.gitlab/frontend.yml` |

这种方式避免每次提交都同时执行前后端任务，能够减少无关构建时间。

---

## 4. 后端 CI 配置

后端流水线定义在 `.gitlab/backend.yml`。

### 4.1 阶段

```text
build -> test
```

### 4.2 任务清单

| 任务 | 阶段 | 镜像 | 命令 | 作用 |
|------|------|------|------|------|
| `backend-build` | build | `maven:3.9.9-eclipse-temurin-17` | `mvn -B -q clean compile` | 编译后端项目，验证代码可构建 |
| `backend-unit-test` | test | `maven:3.9.9-eclipse-temurin-17` | `mvn -B -q -Dtest=OrderServiceTest,AdminServiceTest test` | 执行服务层单元测试 |
| `backend-integration-test` | test | `maven:3.9.9-eclipse-temurin-17` | `mvn -B -q -Dtest=CoreFlowIntegrationTest test` | 执行核心流程集成测试 |

### 4.3 缓存与产物

| 类型 | 内容 |
|------|------|
| Maven 缓存 | `.m2/repository` |
| 构建产物 | `backend/target` |
| 测试报告 | `backend/target/surefire-reports` |
| 产物保留时间 | 1 week |

---

## 5. 前端 CI 配置

前端流水线定义在 `.gitlab/frontend.yml`。

### 5.1 阶段

```text
build
```

### 5.2 任务清单

| 任务 | 阶段 | 镜像 | 命令 | 作用 |
|------|------|------|------|------|
| `frontend-build` | build | `node:20` | `npm ci`、`npm run build` | 安装依赖，执行 Vue 类型检查和生产构建 |

前端 `npm run build` 实际执行：

```bash
vue-tsc --noEmit && vite build
```

因此该任务同时覆盖：

1. TypeScript 类型检查。
2. Vue 单文件组件类型检查。
3. Vite 生产构建。

### 5.3 缓存与产物

| 类型 | 内容 |
|------|------|
| Node 缓存 | `frontend/node_modules` |
| 构建产物 | `frontend/dist` |
| 产物保留时间 | 1 week |

---

## 6. 本地等价验证命令

后端：

```bash
cd backend
mvn clean compile
mvn -Dtest=OrderServiceTest,AdminServiceTest test
mvn -Dtest=CoreFlowIntegrationTest test
```

前端：

```bash
cd frontend
npm ci
npm run build
```

---

## 7. 运行记录

| 检查项 | 记录 |
|------|------|
| 后端编译 | 已配置自动执行 |
| 后端单元测试 | 已配置自动执行 |
| 后端集成测试 | 已配置自动执行 |
| 前端依赖安装 | 已配置自动执行 |
| 前端类型检查 | 通过 `npm run build` 自动执行 |
| 前端生产构建 | 已配置自动执行 |
| 测试报告保存 | 已配置 `surefire-reports` 产物 |
| 构建产物保存 | 已配置后端 `target` 和前端 `dist` 产物 |

说明：本文档记录的是仓库中已经提交的 CI/CD 配置。具体某一次 GitLab 页面流水线截图或编号需要以提交到 GitLab 后的实际运行页面为准。

---

## 8. 当前不足与改进计划

当前 CI/CD 已满足 P4 阶段的基础质量检查要求，但仍有以下改进空间：

1. 后端尚未接入 JaCoCo 覆盖率统计。
2. 前端尚未配置独立单元测试或端到端测试。
3. 当前流水线以构建和测试为主，尚未配置自动部署。
4. 顶层流水线只按前后端目录触发，数据库脚本变化暂未单独触发后端测试。

后续可增加覆盖率报告、数据库脚本校验、前端 E2E 测试和演示环境部署任务。
