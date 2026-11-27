# demo 演示辅助脚本

`reset-demo-account.sql`：仅用于 **demo profile 本地演示库**，把教师 `T2001` 恢复为 1000/0，并关闭其当前 OPEN 悬赏。

原因：`DemoDataSeeder` 的 q2 自带一个 50 分 OPEN 悬赏，教师初始会是 950/50；演示“再发起 50 分悬赏”前先执行本脚本，才能得到 950/50、冻结 50 的可复现结果。

执行方式（dev compose）：

```bash
docker exec -i co-mysql-dev mysql -ucampus -pcampus_dev_pwd campus_overflow < scripts/demo/reset-demo-account.sql
```

> 这会直接改 `rep_bounty` / `rep_account`，只适合 demo 库；不要在生产库执行。
