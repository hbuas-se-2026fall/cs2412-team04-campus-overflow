# Grafana 四面板 UI 截图

> 采集：2026-11-19，陈明哲（E）
> 环境：Windows 原生 Grafana **11.3.0** + Prometheus **2.55.1**，后端 `campus-overflow.jar`（demo profile，8080）

## 文件

- [`grafana-four-panels.png`](grafana-four-panels.png)：Dashboard `CampusOverflow · 质量与可观测性`（uid `campus-overflow-obs`）四面板。

## 面板与实测值

| 面板 | Prometheus 指标 | 截图值 |
| --- | --- | --- |
| 发件箱积压 | `co_outbox_backlog` | 0 |
| 死信事件 | `co_outbox_dead_letters` | 0 |
| 声誉对账不一致 | `co_reputation_mismatches` | 0 |
| 接口延迟 P95 | `histogram_quantile(0.95, sum by (le, uri)(rate(http_server_requests_seconds_bucket{uri=~"/api/v1/.*"}[5m]))) * 1000` | `/api/v1/questions` ≈ 11.7 ms |

## 复现

- Dashboard / provisioning 源码：`deploy/grafana/dashboards/campus-overflow.json`、`deploy/grafana/provisioning/`；
- 有 Docker 的机器：`docker compose -f deploy/docker-compose.yml --profile observability up -d --build`；
- 本次无 Docker，使用 Grafana/Prometheus Windows 二进制 + 同一份 provisioning，数据源指向本机 9090/8080。
