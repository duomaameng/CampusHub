# CampusHub 数据库脚本

数据库脚本已经整合为常用入口，不需要再按多个零散补丁逐个执行。

## 文件说明

- `01-schema.sql`：创建数据库以及当前版本的全部表结构。
- `02-seed.sql`：插入本地开发使用的演示账号、任务、订单和业务数据。
- `03-reset-dev-data.sql`：清空开发数据，保留表结构。
- `04-upgrade-existing-db.sql`：将已经存在的旧版开发数据库升级到当前结构，并保留现有数据。
- `05-add-six-open-tasks.sql`：向已有数据库补充六条待接单演示需求及两张二手交易配图，可重复执行。

## 全新初始化

依次执行：

1. `01-schema.sql`
2. `02-seed.sql`

`01-schema.sql` 会重建表结构，不要对需要保留数据的数据库执行。

## 升级当前数据库

如果本地已经有 `campus_hub` 数据和账号，只执行：

1. `04-upgrade-existing-db.sql`

该脚本会补齐订单超时举报等当前结构，并更新演示任务配图，不会清空现有业务数据。

如果只需要补充六条待接单需求，直接执行：

1. `05-add-six-open-tasks.sql`

## 重置开发数据

依次执行：

1. `03-reset-dev-data.sql`
2. `02-seed.sql`

## 数据库连接

后端通过环境变量读取 MySQL 密码：

```text
DB_PASSWORD=你的MySQL密码
```

配置完成后重启后端。
