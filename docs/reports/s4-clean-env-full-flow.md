# Sprint 4 · 干净环境全流程验证（注册→登录→提问→回答→投票→采纳）

> 验证人：陈明哲（E）　日期：2026-11-17
> 关联：T4-01 干净环境部署；[`s4-clean-env-deploy-verify.md`](s4-clean-env-deploy-verify.md)（#150，compose 基础设施）

## 一、环境

| 项 | 值 |
| --- | --- |
| 应用 | `campus-overflow.jar`（v1.0.0 代码），端口 8081 |
| JDK | 21.0.10 |
| 数据库 | MySQL 8.4 临时实例（3307），**全新库 `campus_clean`**（Flyway 自动建表，无演示数据） |
| Redis | 5.0（6379，会话/限流） |
| 冒烟脚本 | `scripts/smoke/smoke.ps1`（T1-15） |

## 二、步骤与结果

```powershell
# 1) 建全新库（Flyway 启动时迁移）
CREATE DATABASE campus_clean ...;

# 2) 启动后端（dev 默认 profile，不带 demo）
java -jar backend/co-bootstrap/target/campus-overflow.jar   # SERVER_PORT=8081, DB_NAME=campus_clean

# 3) 一条命令跑完整链路
powershell -File scripts/smoke/smoke.ps1 -BaseUrl http://127.0.0.1:8081
```

输出：

```text
[1/9] 获取 CSRF
[2/9] 注册提问者 smokeA1117160131969
[3/9] 登录提问者
[4/9] 发布问题
[5/9] 注册回答者 smokeB1117160131969
[6/9] 切换登录回答者并提交回答
[7/9] 回答者给问题投 +1
[8/9] 切回提问者并采纳回答
[9/9] 读取详情并校验

SMOKE PASS
  用户 A=smokeA1117160131969 (id=1)  用户 B=smokeB1117160131969 (id=2)
  问题 id=1  回答 id=1  score=1  acceptedAnswerId=1
  耗时 4.3s  base=http://127.0.0.1:8081
```

用户 id=1/2、问题/回答 id=1 说明库是全新的，注册、登录、发问、回答、投票、采纳全部真实落库。

## 三、与 compose 证据的关系

- compose 基础设施（MySQL/Redis 容器 healthy、后端启动、health=UP、Flyway/演示数据可读）已由 [#150](https://github.com/hbuas-se-2026fall/cs2412-team04-campus-overflow/pull/150) 在 Docker Desktop 29.9.3 上验证；
- 本文件补充「全新库全流程」证据，两者合起来覆盖 sprint-4「干净环境按 README 启动并完成注册→登录→提问→回答」验收。
- 说明：本次全流程的后端以 jar 启动（本机无 Docker），compose 只起 MySQL/Redis；全栈 compose（backend/web 镜像）的运行时已由房国栋于 2026-11-18 单独验证，见 [`s4-compose-fullstack-verify.md`](s4-compose-fullstack-verify.md)（四容器、Nginx 80 反代、health=UP）。

## 四、结论

- [x] 干净库启动（Flyway 迁移成功）
- [x] 注册 / 登录 / 发问 / 回答 / 投票 / 采纳全链路 `SMOKE PASS`
- [x] 数据落库（问题/回答 id=1，acceptedAnswerId 生效）
