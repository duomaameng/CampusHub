# 用例建模

**团队：** [您的团队名] | **日期：** 2026年4月19日

## 1. 系统级用例图（Mermaid）

```mermaid
graph TD
  actor "需求方" as A
  actor "服务方" as B
  actor "管理员" as C

  A --> UC1[注册/登录]
  A --> UC2[发布需求]
  A --> UC3[浏览需求]
  A --> UC4[确认完成]
  A --> UC5[评价订单]
  A --> UC6[发送消息]

  B --> UC1
  B --> UC3
  B --> UC7[接单]
  B --> UC4
  B --> UC5
  B --> UC6

  C --> UC8[用户管理]
  C --> UC9[内容审核]
  C --> UC10[查看数据看板]