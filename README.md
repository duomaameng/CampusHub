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

## 环境要求

- Node.js 18+
- Java 17+
- Maven 3.9+
- MySQL 8.0

---

## 前端本地运行

首次运行时，在项目根目录复制前端配置示例：

```powershell
Copy-Item frontend/.env.example frontend/.env.local
```

示例配置默认关闭 Mock，并通过 Vite 代理访问本地后端：

```env
VITE_USE_MOCK=false
VITE_API_BASE_URL=/api
VITE_ASSET_BASE_URL=http://localhost:8080
```

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
| 学生 | `student.demo3@smail.nju.edu.cn` | `CampusHub123!` |

### 启动后端

首次启动前，在项目根目录复制后端配置示例：

```powershell
Copy-Item backend/.env.example backend/.env
```

然后打开 `backend/.env`，将 `DB_PASSWORD` 改为本机 MySQL 密码：

```properties
DB_PASSWORD=你的MySQL密码
MAIL_DELIVERY_MODE=log
```

`MAIL_DELIVERY_MODE=log` 会把验证码打印到后端日志，助教无需配置 SMTP 邮箱即可检查注册等流程。`backend/.env` 仅用于本地配置且不会提交到 Git；也可以不创建该文件，改为在启动后端前设置同名环境变量。

```bash
cd backend
mvn spring-boot:run
```

默认运行在 [http://localhost:8080](http://localhost:8080)。

启动成功后，可通过 [Swagger UI](http://localhost:8080/swagger-ui.html) 检查后端接口。

后端支持以下环境变量：

| 变量 | 默认值/要求 | 说明 |
|------|-------------|------|
| `DB_HOST` | `localhost` | MySQL 主机名；Docker 部署时为 `db` |
| `DB_PORT` | `3306` | MySQL 端口 |
| `DB_NAME` | `campus_hub` | MySQL 数据库名 |
| `DB_USERNAME` | `root` | MySQL 用户名 |
| `DB_PASSWORD` | 必填 | `DB_USERNAME` 对应的 MySQL 密码；本地默认是 `root` 用户，可写入 `backend/.env` |
| `JWT_SECRET` | 内置默认值 | JWT 签名密钥 |
| `MAIL_DELIVERY_MODE` | `smtp` | 验证码发送方式，`smtp` 真发邮件，`log` 仅打印验证码 |
| `MAIL_HOST` | `smtp.qq.com` | 邮件服务地址 |
| `MAIL_PORT` | `465` | 邮件服务端口，默认使用 SSL |
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

如果没有在“前端本地运行”步骤中创建配置，可在项目根目录复制：

```powershell
Copy-Item frontend/.env.example frontend/.env.local
```

确认 `frontend/.env.local` 内容如下：

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

## 云服务器部署

仓库提供 Docker Compose 生产部署配置，包含 Vue/Nginx、Spring Boot、MySQL，以及数据库和上传文件持久卷。

项目访问地址：[http://campushub-nju.eastasia.cloudapp.azure.com](http://campushub-nju.eastasia.cloudapp.azure.com)。

> 注意：服务器每天凌晨 2 点自动关闭，关闭后上述地址将暂时无法访问。

完整步骤见 [`deploy/README.md`](deploy/README.md)，其中包含现有数据库导出、迁移、恢复、备份和答辩证据清单。

---

## 验证

```bash
# 前端类型检查与构建
cd frontend
npm run typecheck
npm run build

# 后端编译
cd ../backend
mvn compile
```
