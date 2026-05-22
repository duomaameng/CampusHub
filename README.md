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
| `MAIL_HOST` | `smtp.qq.com` | 邮件服务器地址 |
| `MAIL_USERNAME` | 空 | 邮箱账号 |
| `MAIL_PASSWORD` | 空 | 邮箱授权码 |

---

## 前后端联调

在 `frontend` 目录创建 `.env.local`：

```env
VITE_USE_MOCK=false
VITE_API_BASE_URL=http://localhost:8080/api
```

然后重启前端 `npm run dev`。

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
