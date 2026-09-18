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
