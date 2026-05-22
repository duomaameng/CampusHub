# 数据库初始化说明

本目录存放 CampusHub 本地开发使用的数据库脚本。

## 文件说明

- `01-schema.sql`
  - 创建 `campus_hub` 数据库
  - 创建项目核心表结构

- `02-seed.sql`
  - 插入测试账号
  - 插入示例任务、申请、订单、通知、评价、举报、公告等测试数据

- `03-reset-dev-data.sql`
  - 清空当前开发测试数据
  - 适合在需要重新导入 `02-seed.sql` 前执行

## 当前测试账号

当前共享测试密码统一为：

`CampusHub123!`

测试账号如下：

- `admin.demo@smail.nju.edu.cn`
- `student.demo1@smail.nju.edu.cn`
- `student.demo2@smail.nju.edu.cn`
- `student.pending@smail.nju.edu.cn`

## 手动执行顺序

首次初始化数据库：

1. 执行 `01-schema.sql`
2. 执行 `02-seed.sql`

如果需要重置测试数据：

1. 执行 `03-reset-dev-data.sql`
2. 再执行 `02-seed.sql`

## 使用 MySQL Workbench 导入

如果你们组员都使用 MySQL Workbench，可以按下面步骤操作：

1. 打开 MySQL Workbench
2. 连接本地 MySQL 实例
3. 打开 `01-schema.sql`
4. 点击执行，完成建库建表
5. 再打开 `02-seed.sql`
6. 点击执行，导入测试数据

如果后续要重置测试数据：

1. 先执行 `03-reset-dev-data.sql`
2. 再执行 `02-seed.sql`

## 后端数据库密码配置

后端推荐通过环境变量配置数据库密码，不要直接把真实密码写死在 `application.yml` 里。

`application.yml` 中推荐写法如下：

```yml
spring:
  datasource:
    password: ${DB_PASSWORD}
```

### 在 IDEA 中配置 `DB_PASSWORD`

1. 打开 IDEA
2. 点击右上角运行配置下拉框，选择 `编辑配置`
3. 选择 `CampusHubApplication`
4. 在右侧点击 `环境变量`
5. 新增一条变量：

```text
DB_PASSWORD=你的 MySQL 密码
```

例如：

```text
DB_PASSWORD=147359Wss@&
```

6. 点击 `确定`
7. 回到运行配置窗口后再次点击 `确定`
8. 重启 `CampusHubApplication`

这样后端启动时就会自动读取数据库密码。

## PowerShell 辅助脚本

如果不想在 Workbench 里手动执行，也可以使用脚本：

- 初始化数据库：

```powershell
.\scripts\init-db.ps1 -Mode init -Username root -Password "你的MySQL密码"
```

- 重置开发测试数据：

```powershell
.\scripts\init-db.ps1 -Mode reset-seed -Username root -Password "你的MySQL密码"
```

如果 MySQL 安装在常见路径下，一般不需要额外传 `-MySqlExePath`。
