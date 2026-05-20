# 数据库初始化说明

这份文档用于帮助组员在本地快速完成 `CampusHub` 数据库初始化。

如果你已经安装了 `MySQL Workbench` 和本地 `MySQL Server`，推荐优先使用 **Workbench 手动导入**。  
根目录里的 `init-db.ps1` 脚本是辅助方案，不会用也没关系。

## 1. 需要用到的文件

数据库相关文件都在 [database](C:\Users\duoma\java\软工2项目\CampusHub\database) 目录下：

- [01-schema.sql](C:\Users\duoma\java\软工2项目\CampusHub\database\01-schema.sql)
  - 用来建库、建表
- [02-seed.sql](C:\Users\duoma\java\软工2项目\CampusHub\database\02-seed.sql)
  - 用来插入基础测试数据
- [03-reset-dev-data.sql](C:\Users\duoma\java\软工2项目\CampusHub\database\03-reset-dev-data.sql)
  - 用来清空开发测试数据，方便重新导入

如果你只是第一次初始化数据库，只需要运行前两个文件：

1. `01-schema.sql`
2. `02-seed.sql`

## 2. 初始化前的准备

请先确认：

- 本机已经安装并启动 `MySQL Server`
- 能通过 `MySQL Workbench` 正常连接本地数据库
- 知道自己的 MySQL 用户名和密码
  - 默认常见用户名是 `root`

## 3. 用 MySQL Workbench 初始化数据库

这是最推荐的方式。

### 第一步：打开本地连接

打开 `MySQL Workbench`，进入你自己的本地连接，例如：

- `Local instance MySQL80`

### 第二步：导入建表脚本

1. 点击菜单栏 `File -> Open SQL Script...`
2. 打开 [01-schema.sql](C:\Users\duoma\java\软工2项目\CampusHub\database\01-schema.sql)
3. 点击执行按钮运行整个脚本

执行成功后，会完成：

- 创建数据库 `campus_hub`
- 创建项目所需的表结构

### 第三步：导入测试数据

1. 再次点击 `File -> Open SQL Script...`
2. 打开 [02-seed.sql](C:\Users\duoma\java\软工2项目\CampusHub\database\02-seed.sql)
3. 点击执行按钮运行整个脚本

执行成功后，会插入基础测试数据，例如：

- 管理员账号
- 普通用户账号
- 示例任务
- 示例订单
- 示例通知

## 4. 如何确认初始化成功

你可以在 Workbench 左侧 `Schemas` 里刷新，确认出现：

- `campus_hub`

展开后应能看到项目中的主要表，例如：

- `user`
- `user_profile`
- `task`
- `application`
- `orders`
- `order_message`
- `notification`
- `review`
- `report`
- `announcement`

如果这些表都存在，说明建表基本成功。

你也可以执行下面这类查询简单检查：

```sql
USE campus_hub;

SELECT COUNT(*) FROM user;
SELECT COUNT(*) FROM task;
SELECT COUNT(*) FROM orders;
```

如果能查到数据，说明测试数据也已经导入成功。

## 5. 如果要重置测试数据

如果你已经导入过一次测试数据，后面想重新回到“初始演示状态”，不要重复运行旧版带删除逻辑的脚本。  
现在推荐的做法是：

1. 先运行 [03-reset-dev-data.sql](C:\Users\duoma\java\软工2项目\CampusHub\database\03-reset-dev-data.sql)
2. 再运行 [02-seed.sql](C:\Users\duoma\java\软工2项目\CampusHub\database\02-seed.sql)

这样会：

- 清空开发测试数据
- 再重新灌入一套统一测试数据

## 6. `init-db.ps1` 脚本是做什么的

根目录的脚本在这里：

- [init-db.ps1](C:\Users\duoma\java\软工2项目\CampusHub\scripts\init-db.ps1)

它的作用是：

- 自动调用 MySQL 客户端
- 自动执行 `01-schema.sql`
- 自动执行 `02-seed.sql`

也就是说，它相当于把“手动点 Workbench 导入”的过程改成命令行一键执行。

如果你平时主要用 Workbench，这个脚本不是必须的。

## 7. 如果想用脚本初始化

先进入项目根目录：

```powershell
cd C:\Users\duoma\java\软工2项目\CampusHub
```

然后执行：

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\scripts\init-db.ps1 -Username root -Password "你的MySQL密码"
```

如果以后只想重置测试数据：

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\scripts\init-db.ps1 -Mode reset-seed -Username root -Password "你的MySQL密码"
```

说明：

- `-Scope Process` 表示只对当前 PowerShell 窗口临时生效
- 关闭窗口后不会永久修改系统策略

## 8. 常见问题

### 8.1 `safe update mode` 报错

如果你在 Workbench 里运行删除脚本时遇到：

- `Error Code: 1175. You are using safe update mode ...`

说明当前 Workbench 开启了安全更新模式。

解决方法有两个：

1. 优先使用我们现在拆开的脚本流程，不要自己乱删数据
2. 如果确实要执行重置脚本，可以在 Workbench 里临时关闭 safe update mode

不过在正常情况下，按：

1. `01-schema.sql`
2. `02-seed.sql`

第一次初始化时，不会遇到这个问题。

### 8.2 PowerShell 提示“禁止运行脚本”

这是 Windows 的执行策略限制，不是脚本坏了。

先执行：

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
```

然后再执行 `init-db.ps1`。

### 8.3 不知道用户名是不是 `root`

可以在 `MySQL Workbench` 首页：

1. 右键本地连接
2. 选择 `Edit Connection...`
3. 查看 `Username`

如果没有改过，很多本地 MySQL 默认就是 `root`。

### 8.4 导入脚本时报语法错误

优先检查：

- 是不是没先运行 `01-schema.sql`
- 运行顺序是否正确
- 是否只执行了部分脚本
- 当前连接是不是本地 MySQL，而不是别的远程库

如果是脚本工具方式报错，也可以先退回到 Workbench 手动导入。

## 9. 推荐做法

对于组员来说，最推荐的数据库初始化方式是：

1. 用 `MySQL Workbench` 打开 [01-schema.sql](C:\Users\duoma\java\软工2项目\CampusHub\database\01-schema.sql)
2. 执行建库建表
3. 用 `MySQL Workbench` 打开 [02-seed.sql](C:\Users\duoma\java\软工2项目\CampusHub\database\02-seed.sql)
4. 执行测试数据导入

这样最直观，也最不容易被脚本权限或命令行问题卡住。

如果后面需要重复初始化，再考虑用：

- `03-reset-dev-data.sql`
- 或 `init-db.ps1`

