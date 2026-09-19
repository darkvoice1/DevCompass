# 首页焦点清单

首页用同一个接口分别查询本周任务、逾期事项、即将到期任务和阻塞任务。日期边界按配置时区 `Asia/Shanghai` 计算“今天”，再把日期传给数据库。

## 查询接口

```text
GET /api/v1/dashboard/focus-lists?type={type}
```

`type` 必填，取值：

| 类型 | 含义 | 日期范围 |
| --- | --- | --- |
| `THIS_WEEK` | 本周到期的未完成任务 | 本周一到周日，包含今天 |
| `OVERDUE` | 已经晚了的任务，以及目标日期已过的未完成项目 | 截止日期或项目目标日期早于今天 |
| `DUE_SOON` | 即将到期的未完成任务 | 今天到未来 7 天，包含今天和第 7 天 |
| `BLOCKED` | 已标记阻塞的未完成任务 | 不按日期筛选 |

今天到期的任务算即将到期，不算逾期。

## 共同规则

- 排除已归档、已软删除项目，以及已完成、已取消、已软删除任务。
- 没有截止日期的任务不进入本周、逾期、即将到期清单；阻塞清单不要求截止日期。
- 已完成项目即使目标日期过了，也不会进入逾期清单。
- 同一条任务可以同时出现在多份清单里。例如既逾期又阻塞时，两份清单都会看到它。
- `itemKind=TASK` 用 `taskId` 和 `projectId` 跳转任务；`itemKind=PROJECT` 只有 `projectId`，跳转项目。

## 标记阻塞

创建或编辑任务时可以标记阻塞：

```json
{
  "blocked": true,
  "blockerReason": "依赖登录接口"
}
```

`blockerReason` 可选，最长 500 个字符。传入 `"blocked": false` 会取消阻塞并清空原因。

## 响应示例

```text
GET /api/v1/dashboard/focus-lists?type=OVERDUE
```

```json
{
  "code": "0",
  "message": "success",
  "data": {
    "type": "OVERDUE",
    "fromDate": null,
    "toDate": "2026-09-18",
    "items": [
      {
        "itemKind": "TASK",
        "taskId": 1,
        "title": "逾期任务",
        "status": "TODO",
        "dueDate": "2026-09-15",
        "projectId": 10,
        "projectName": "研发罗盘",
        "blockerReason": null
      },
      {
        "itemKind": "PROJECT",
        "taskId": null,
        "title": "延期项目",
        "status": null,
        "dueDate": "2026-09-01",
        "projectId": 20,
        "projectName": "延期项目",
        "blockerReason": null
      }
    ]
  }
}
```
