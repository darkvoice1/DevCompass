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
