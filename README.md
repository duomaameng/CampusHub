# CampusHub

CampusHub 是面向南京大学学生的校园互助服务平台。

**技术栈：** Vue3 + TypeScript (前端) / Spring Boot 3.2 + MyBatis-Plus (后端) / MySQL 8.0 (数据库)

---

## 项目结构

```
campus-hub/
├── frontend/          # Vue3 前端
├── backend/           # Spring Boot 后端
├── database/          # 建库建表 + 测试数据 SQL
├── docs/              # P0-P4 阶段文档（需求、架构、设计、任务看板）
└── scripts/           # 辅助脚本
```

---

## 前端本地运行

```bash
cd frontend
npm install
npm run dev
```

打开 `http://localhost:5173`。

默认 Mock 演示账号：

| 角色 | 邮箱 | 密码 |
|------|------|------|
| 学生 | cailiyang@smail.nju.edu.cn | Abc123456! |
| 管理员 | admin@smail.nju.edu.cn | Admin123456! |

---

## 后端本地运行

### 环境要求

- Java 17+
- Maven 3.9+
- MySQL 8.0

### 初始化数据库

```bash
# 1. 建库建表
mysql -u root -p < database/01-schema.sql

# 2. 导入测试数据
mysql -u root -p < database/02-seed.sql
```

测试账号密码均为 `Password123!`：

| 角色 | 邮箱 | 密码 |
|------|------|------|
| 管理员 | admin@smail.nju.edu.cn | Password123! |
| 学生 | cailiyang@smail.nju.edu.cn | Password123! |
| 学生 | wangzikuan@smail.nju.edu.cn | Password123! |
| 学生 | wangshengsheng@smail.nju.edu.cn | Password123! |

### 启动后端

```bash
cd backend
mvn spring-boot:run
```

默认运行在 `http://localhost:8080`。

可通过环境变量覆盖配置：

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `DB_PASSWORD` | `root` | MySQL 密码 |
| `JWT_SECRET` | 内置默认值 | JWT 签名密钥 |
| `MAIL_DELIVERY_MODE` | `smtp` | 验证码发送方式，`smtp` 真发邮件，`log` 仅打印验证码 |
| `MAIL_HOST` | `smtp.qq.com` | 邮件服务地址 |
| `MAIL_PORT` | `587` | 邮件服务端口 |
| `MAIL_USERNAME` | - | 邮箱账号 |
| `MAIL_PASSWORD` | - | 邮箱授权码 |
| `MAIL_FROM` | `MAIL_USERNAME` | 邮件发件人地址 |

本地联调真实后端时，如需真的发送验证码邮件，可临时使用个人邮箱的 SMTP 配置：

```powershell
$env:MAIL_DELIVERY_MODE="smtp"
$env:MAIL_USERNAME="your-mail@qq.com"
$env:MAIL_PASSWORD="你的 SMTP 授权码"
$env:MAIL_FROM="your-mail@qq.com"
mvn spring-boot:run
```

部署到服务器时不需要改代码，只需把上述环境变量换成服务器或团队邮箱配置。若暂时没有 SMTP 服务，可设置 `MAIL_DELIVERY_MODE=log`，验证码会打印在后端日志中。若使用 QQ 邮箱等服务，请填写邮箱服务商提供的 SMTP 授权码，而不是邮箱登录密码。

---

## 前后端联调

在 `frontend` 目录创建 `.env.local`：

```env
VITE_USE_MOCK=false
VITE_API_BASE_URL=http://localhost:8080/api
```

然后重启前端 `npm run dev`，所有请求将转发到后端 8080 端口。

当前开发阶段（第一阶段已完成）：

| 阶段 | 状态 |
|------|------|
| 工程骨架 (后端 Spring Boot) | 已完成 |
| 工程骨架 (前端 Vue3) | 已完成 |
| 数据库初始化 (DDL + Seed) | 已完成 |
| 用户认证与权限 | 待开发 |
| 后续模块 | 待开发 |

---

## 验证

```bash
# 前端类型检查与构建
cd frontend
npm run typecheck
npm run build

# 后端编译
cd backend
mvn compile
```
