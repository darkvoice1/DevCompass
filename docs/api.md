# DevCompass API 文档

## Swagger UI

启动应用后访问：

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON 地址：

```text
http://localhost:8080/v3/api-docs
```

## 健康检查

```text
GET /api/v1/health
```

成功响应：

```json
{
  "code": "0",
  "message": "success",
  "data": {
    "status": "UP"
  }
}
```

## 参数校验示例

```text
POST /api/v1/validation/demo
Content-Type: application/json
```

请求体：

```json
{
  "name": "DevCompass"
}
```

## 多项目仪表盘

```text
GET /api/v1/dashboard/projects
```

可选查询参数：`status`、`tag`、`activeWithinDays`。

一次请求返回项目总数、状态分布、健康度分布和项目摘要。详细规则见 [dashboard.md](dashboard.md)。

## 首页焦点清单

```text
GET /api/v1/dashboard/focus-lists?type=THIS_WEEK
```

`type` 必填，可选 `THIS_WEEK`、`OVERDUE`、`DUE_SOON`、`BLOCKED`。

日期按 `Asia/Shanghai` 计算。详细规则见 [focus-list.md](focus-list.md)。

## 全局搜索

```text
GET /api/v1/search?keyword=接口
```

可选查询参数：`keyword`、`type`、`projectId`、`status`、`dateFrom`、`dateTo`。

`type` 可选 `PROJECT`、`TASK`、`WORK_LOG`。第一版使用 PostgreSQL，不引入 Elasticsearch。详细规则见 [search-design.md](search-design.md)。

## 时间线

```text
GET /api/v1/timeline?view=WEEK&date=2026-09-16
```

也可传 `fromDate`、`toDate`，或 `view=MONTH`。返回任务截止日期和项目目标日期。不同步外部日历。详细规则见 [api/timeline.md](api/timeline.md)。

## 项目动态

```text
GET /api/v1/activities?projectId=8
```

`projectId` 必填。可选查询参数：`objectType`、`dateFrom`、`dateTo`、`page`、`pageSize`。

不传 `objectType` 时返回该项目下项目、任务和阶段的动态。只记宏观操作，不记代码 diff。详细规则见 [audit.md](audit.md)。

## 项目附件

```text
POST /api/v1/projects/{projectId}/attachments
Content-Type: multipart/form-data
```

表单字段名是 `file`。成功时返回附件编号、原文件名、类型、大小和上传时间，不返回磁盘路径。

```text
GET /api/v1/projects/{projectId}/attachments
```

返回该项目未删除的附件，新上传的在前。已归档项目仍可上传和查看。项目不存在或已删除时拒绝。单个文件默认不超过 10MB。

```text
GET /api/v1/projects/{projectId}/attachments/{attachmentId}/content
```

响应体是文件本身，浏览器按原文件名下载。附件不属于该项目时当作不存在。

```text
DELETE /api/v1/projects/{projectId}/attachments/{attachmentId}
```

成功时 `data` 为 `null`。记录标成已删除，并删掉磁盘文件。删除后不能再下载，这一版不能恢复。允许的文件和存储位置见 [attachment.md](attachment.md)。

## 项目导出

```text
GET /api/v1/projects/{projectId}/export
```

下载一份 JSON 文件，文件名是 `project-{projectId}.json`。里面有 `formatVersion`、项目、阶段、任务、工作日志和动态。附件只含原文件名、类型和大小，不含文件内容，也不含磁盘路径。已删除的数据不导出。已归档项目可以导出。

```text
GET /api/v1/projects/{projectId}/tasks/export
```

下载 `project-{projectId}-tasks.csv`，用 Excel 打开任务清单。列是标题、状态、所属阶段名称、优先级、截止日期、是否阻塞。阶段只写名称。项目不存在时拒绝。整库备份、恢复和附件文件夹的复制见 [backup.md](backup.md)。

```text
POST /api/v1/projects/import
Content-Type: application/json
```

请求体就是上面导出的 JSON。`formatVersion` 必须是 `1`。同名项目已存在时整份拒绝，不覆盖旧数据。成功后数据库重新分配编号，阶段、任务、日志和动态会按文件里的旧编号重新连上。附件不导入，响应里的 `skippedAttachmentCount` 是跳过的附件数量。

```json
{"code":"0","message":"success","data":{"projectId":100,"projectName":"研发罗盘","skippedAttachmentCount":1}}
```
