# 时间线接口

按日期范围返回任务截止日期和项目目标日期，供前端画周视图或月视图。本接口只提供数据，不同步外部日历。

仓库没有里程碑表。项目目标日期代替里程碑节点。

## 查询接口

```text
GET /api/v1/timeline
```

两种用法，满足一种即可：

| 参数 | 说明 |
| --- | --- |
| `fromDate` | 开始日期，和 `toDate` 一起使用，包含当天 |
| `toDate` | 结束日期，和 `fromDate` 一起使用，包含当天 |
| `view` | `WEEK` 或 `MONTH` |
| `date` | 周/月视图的锚点日期；不传则用 `Asia/Shanghai` 的今天 |

规则：

- 同时传了 `fromDate` 和 `toDate` 时，以这段范围为准，忽略 `view`。
- 只传 `view=WEEK`：该日所在周一到周日，和本周清单一致。
- 只传 `view=MONTH`：该日所在月的 1 号到月末。
- 既没有完整日期范围，也没有 `view` 时返回校验错误。
- 开始日期不能晚于结束日期。
- 响应里的 `fromDate`、`toDate` 是实际使用的范围，前端可用来画表头。

## 事件规则

| 类型 | 日期 | 完成标记 |
| --- | --- | --- |
| `TASK` | 任务截止日期 | 任务状态是 `COMPLETED` |
| `PROJECT` | 项目目标日期 | 项目状态是 `COMPLETED` |

- 没有截止日期的任务、没有目标日期的项目不出现。
- 已取消、已删除任务不出现；已归档、已删除项目及其任务不出现。
- 已完成的任务和项目会出现，并用 `completed=true` 标记。
- 跨月只是起止日期跨过月底，不会按自然月截断。
- 结果按日期从早到晚排列。同一天时项目排在任务前面。
- 项目事件没有任务的 `status`、`priority`，这两项为 `null`。

## 请求示例

查某一周：

```text
GET /api/v1/timeline?view=WEEK&date=2026-09-16
```

实际范围是 `2026-09-14` 到 `2026-09-20`。

查某一月：

```text
GET /api/v1/timeline?view=MONTH&date=2026-09-16
```

跨月自定义范围：

```text
GET /api/v1/timeline?fromDate=2026-08-20&toDate=2026-09-10
```

```json
{
  "code": "0",
  "message": "success",
  "data": {
    "fromDate": "2026-08-20",
    "toDate": "2026-09-10",
    "items": [
      {
        "type": "PROJECT",
        "id": 8,
        "title": "研发罗盘",
        "date": "2026-08-31",
        "projectId": 8,
        "projectName": "研发罗盘",
        "status": null,
        "priority": null,
        "completed": false
      },
      {
        "type": "TASK",
        "id": 1,
        "title": "九月初任务",
        "date": "2026-09-05",
        "projectId": 8,
        "projectName": "研发罗盘",
        "status": "TODO",
        "priority": "MEDIUM",
        "completed": false
      }
    ]
  }
}
```

看到 `type=TASK` 用 `id` 跳任务；看到 `type=PROJECT` 用 `id` 跳项目。
