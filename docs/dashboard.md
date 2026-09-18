# 多项目仪表盘

首页一次请求返回未归档项目的数量、状态分布、健康度分布和项目摘要。整个请求只执行一次数据库查询。

## 业务规则

- 排除已归档和已软删除项目。
- 自动进度模式使用 `autoProgress`，手动进度模式使用 `manualProgress`。
- 四种项目状态始终返回：`PLANNED`、`IN_PROGRESS`、`COMPLETED`、`PAUSED`。没有对应项目时数量为 0。
- 三种健康度始终返回：`HEALTHY`、`AT_RISK`、`OVERDUE`。没有对应项目时数量为 0。
- 筛选条件都是可选的，多个条件同时生效时使用 AND 组合。
- 返回的项目总数、状态分布和健康度分布都按筛选后的结果统计。

## 查询接口

```text
GET /api/v1/dashboard/projects
```

查询参数：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `status` | 否 | 项目状态：`PLANNED`、`IN_PROGRESS`、`COMPLETED` 或 `PAUSED` |
| `tag` | 否 | 按完整项目标签筛选，最长 500 个字符；会忽略标签两侧空格 |
| `activeWithinDays` | 否 | 最近活跃天数，最小值为 1 |

标签匹配不会把“后”匹配成“后端”。

最近活跃时间取以下时间中的最大值：

- 项目更新时间
- 项目下未删除任务的更新时间
- 这些任务下未删除工作日志的更新时间

示例：

```text
GET /api/v1/dashboard/projects?status=IN_PROGRESS&tag=后端&activeWithinDays=30
```

## 健康度

健康度根据已保存的任务截止日期和项目目标日期计算，不会自动生成日期。

- `OVERDUE`（延期）：有未完成且截止日期已过的任务；或者项目还没完成，但项目目标日期已经过了。
- `AT_RISK`（预警）：没有延期，但有任务会在今天起 7 天内到期；或者未完成项目的目标日期落在这 7 天内。
- `HEALTHY`（健康）：上面两种情况都没有。

补充规则：

- 已完成、已取消、已软删除的任务不计入逾期和即将到期。
- 没有截止日期的任务不计入逾期和即将到期。
- 已完成项目即使目标日期过了，只要没有逾期任务，仍视为健康。
- 延期优先于预警：同时有逾期任务和即将到期任务时，结果是延期。
- 任务逾期会导致项目延期；项目目标日期过了，即使没有逾期任务，项目也会延期。

## 响应示例

```json
{
  "code": "0",
  "message": "success",
  "data": {
    "totalProjects": 1,
    "statusDistribution": {
      "PLANNED": 0,
      "IN_PROGRESS": 1,
      "COMPLETED": 0,
      "PAUSED": 0
    },
    "healthDistribution": {
      "HEALTHY": 0,
      "AT_RISK": 0,
      "OVERDUE": 1
    },
    "projects": [
      {
        "id": 1,
        "name": "研发罗盘",
        "status": "IN_PROGRESS",
        "progress": 40,
        "tags": "后端,学习项目",
        "healthStatus": "OVERDUE",
        "overdueTaskCount": 2,
        "dueSoonTaskCount": 1,
        "updatedAt": "2026-09-17T08:00:00Z"
      }
    ]
  }
}
```
