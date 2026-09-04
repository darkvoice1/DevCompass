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
