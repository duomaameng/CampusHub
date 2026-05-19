# CampusHub

CampusHub 是面向南京大学学生的校园互助服务平台。当前仓库已补充 P4 阶段前端工程，前端默认使用 mock 数据运行，后端完成后可切换到真实 API。

## 前端本地运行

```powershell
cd frontend
npm install
npm run dev
```

打开浏览器访问 `http://localhost:5173`。

默认演示账号：

| 角色 | 邮箱 | 密码 |
|------|------|------|
| 学生 | cailiyang@smail.nju.edu.cn | Abc123456! |
| 管理员 | admin@smail.nju.edu.cn | Admin123456! |

## 切换真实后端接口

在 `frontend` 目录创建 `.env.local`：

```env
VITE_USE_MOCK=false
VITE_API_BASE_URL=http://localhost:8080/api
```

然后重新运行：

```powershell
npm run dev
```

## 前端验证

```powershell
cd frontend
npm run typecheck
npm run build
```
