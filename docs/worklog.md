# 工作日志

## 业务规则

- 每个任务最多对应一份工作日志。
- 将任务状态改为 `COMPLETED` 时，必须同时填写工作日志。
- 日志保存和任务状态更新在同一事务中执行；任一步失败时，两项操作都会回滚。
- 任务重新打开后再次完成时，会更新原有日志，不会创建第二份。

## 完成任务并填写日志

```text
PATCH /api/v1/tasks/{taskId}/status
Content-Type: application/json
```

```json
{
  "targetStatus": "COMPLETED",
  "completionLog": {
    "logDate": "2026-09-16",
    "planContent": "完成工作日志接口",
    "summaryContent": "已完成接口和测试",
    "commitHashes": "2dfd4ff,ff072e1,062f73d",
    "spentMinutes": 90,
    "blockerReason": "暂无阻塞"
  }
}
```

`logDate`、`summaryContent`、`commitHashes` 和 `spentMinutes` 必填；耗时单位为分钟。
`commitHashes` 支持多个短哈希，以英文逗号分隔；每个短哈希为 7 至 12 位十六进制字符。

## 查询和编辑

```text
GET /api/v1/work-logs/tasks/{taskId}
PUT /api/v1/work-logs/{workLogId}
GET /api/v1/work-logs?logDateFrom=2026-09-01&logDateTo=2026-09-30
```

日期范围查询要求同时提供 `logDateFrom` 和 `logDateTo`，且开始日期不能晚于结束日期。

## 导出 Markdown

```text
GET /api/v1/work-logs/export?logDateFrom=2026-09-01&logDateTo=2026-09-30
```

接口会以附件形式返回 `.md` 文件，按日志日期分组输出任务、计划、完成总结、提交记录、实际耗时和阻塞原因。
