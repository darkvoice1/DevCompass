# 任务查询接口

## 分页查询

```http
GET /api/v1/tasks/page
```

查询参数：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `projectId` | 是 | 项目 ID |
| `page` | 否 | 页码，从 1 开始，默认 1，最大 1000000 |
| `pageSize` | 否 | 每页数量，默认 20，最大 100 |
| `phaseId` | 否 | 阶段 ID |
| `status` | 否 | `TODO`、`IN_PROGRESS`、`COMPLETED` 或 `CANCELLED` |
| `priority` | 否 | `LOW`、`MEDIUM`、`HIGH` 或 `URGENT` |
| `dueDateFrom` | 否 | 截止日期起始值，格式 `yyyy-MM-dd` |
| `dueDateTo` | 否 | 截止日期结束值，格式 `yyyy-MM-dd` |
| `keyword` | 否 | 按任务标题查询，最长 200 个字符 |
| `sortBy` | 否 | `dueDate`、`updatedAt`、`createdAt`、`priority` 或 `id` |
| `sortDirection` | 否 | `asc` 或 `desc` |

示例：

```http
GET /api/v1/tasks/page?projectId=1&page=1&pageSize=20&status=TODO&phaseId=2&dueDateFrom=2026-01-01&dueDateTo=2026-12-31&keyword=接口&sortBy=dueDate&sortDirection=asc
```

响应示例：

```json
{
  "code": "0",
  "message": "success",
  "data": {
    "records": [],
    "total": 0,
    "page": 1,
    "pageSize": 20,
    "totalPages": 0
  }
}
```

当前任务表已移除标签关联模型，因此本接口暂不支持按标签筛选。
