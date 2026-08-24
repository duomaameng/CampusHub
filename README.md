# CampusHub

CampusHub 是面向南京大学学生的校园互助服务平台。

技术栈：
- 前端：Vue 3 + TypeScript
- 后端：Spring Boot 3.2 + MyBatis-Plus
- 数据库：MySQL 8.0

---

## 项目结构

```text
CampusHub/
|- frontend/          # Vue 3 前端
|- backend/           # Spring Boot 后端
|- database/          # 建库建表与测试数据 SQL
|- docs/              # P0-P4 阶段文档
|- scripts/           # 辅助脚本
```

---

## 前端本地运行

```bash
cd frontend
npm install
npm run dev
```

打开 [http://localhost:5173](http://localhost:5173)。

默认 Mock 演示账号：

| 角色 | 邮箱 | 密码 |
|------|------|------|
| 学生 | `student.demo1@smail.nju.edu.cn` | `CampusHub123!` |
| 管理员 | `admin.demo@smail.nju.edu.cn` | `CampusHub123!` |

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

测试账号统一密码为 `CampusHub123!`：

| 角色 | 邮箱 | 密码 |
|------|------|------|
| 管理员 | `admin.demo@smail.nju.edu.cn` | `CampusHub123!` |
| 学生 | `student.demo1@smail.nju.edu.cn` | `CampusHub123!` |
| 学生 | `student.demo2@smail.nju.edu.cn` | `CampusHub123!` |
| 学生 | `student.pending@smail.nju.edu.cn` | `CampusHub123!` |

### 启动后端

```bash
cd backend
mvn spring-boot:run
```

默认运行在 [http://localhost:8080](http://localhost:8080)。

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
| `FILE_UPLOAD_PATH` | `./uploads` | 后端保存上传图片的磁盘目录 |

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
VITE_ASSET_BASE_URL=http://localhost:8080
```

然后重启前端 `npm run dev`。

### 上传图片与静态资源地址

后端上传接口会把图片保存到 `FILE_UPLOAD_PATH` 指定的目录，并在接口中返回类似 `/uploads/xxx.png` 的相对访问路径。前端会通过 `VITE_ASSET_BASE_URL` 把该路径补全为可访问地址：

```text
/uploads/xxx.png -> http://localhost:8080/uploads/xxx.png
```

本地联调时推荐：

```env
VITE_API_BASE_URL=http://localhost:8080/api
VITE_ASSET_BASE_URL=http://localhost:8080
```

部署时按实际域名配置：

```env
VITE_API_BASE_URL=https://api.example.com/api
VITE_ASSET_BASE_URL=https://api.example.com
```

如果前端和后端部署在同一个域名下，也可以将 `VITE_ASSET_BASE_URL` 配为空或不配置，让 `/uploads/...` 走同源路径。修改 `.env.local` 或部署环境变量后，需要重新启动前端开发服务；生产构建需要重新执行 `npm run build`。

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
