# 全局搜索设计

第一版用 PostgreSQL 做全局搜索，不引入 Elasticsearch。接口一次返回项目、任务和工作日志中的匹配项，方便用一句话找回上下文。

## 查询接口

```text
GET /api/v1/search
```

查询参数都是可选的：

| 参数 | 说明 |
| --- | --- |
| `keyword` | 关键字，最长 200 个字符；空白当作没填 |
| `type` | `PROJECT`、`TASK` 或 `WORK_LOG`；不传则三种都搜 |
| `projectId` | 只看某个项目 |
| `status` | 项目状态或任务状态 |
| `dateFrom` | 起始日期，包含当天 |
| `dateTo` | 结束日期，包含当天 |

多个条件同时生效。没有关键字时，只按筛选条件列出结果。

## 可搜索内容

| 类型 | 关键字匹配字段 | 标题 | 日期筛选 |
| --- | --- | --- | --- |
| `PROJECT` | 名称、描述、标签 | 项目名称 | 项目 `updatedAt` |
| `TASK` | 标题、描述 | 任务标题 | 任务 `updatedAt` |
| `WORK_LOG` | 计划、总结 | 所属任务标题 | 日志 `logDate` |

仓库没有独立笔记表和里程碑表。工作日志当作笔记搜索。

## 筛选规则

- 排除已删除数据；已归档项目及其任务、日志都不出现。
- 关键字按包含匹配，大小写不敏感；`%`、`_` 按普通字符处理。
- 项目和任务的日期按 `Asia/Shanghai` 把日历日换成时间范围，包含当天。
- 工作日志的日期直接比较 `logDate`，包含当天。
- `status=IN_PROGRESS` 时，项目和任务各自匹配进行中。
- `status=TODO` 只筛任务；`status=PLANNED` 只筛项目。
- 工作日志没有状态。传了 `status` 时不搜日志。
- 描述超过 120 字会截成摘要。
- 结果按更新时间从新到旧合并。

## 跳转字段

| 结果类型 | 用哪个 ID 跳转 |
| --- | --- |
| `PROJECT` | `id` 就是项目 ID |
| `TASK` | `id` 是任务 ID，同时带 `projectId` |
| `WORK_LOG` | `id` 是日志 ID，用 `taskId` 跳任务，`projectId` 跳项目 |

## 请求示例

```text
GET /api/v1/search?keyword=接口&type=TASK&projectId=8&status=TODO&dateFrom=2026-09-01&dateTo=2026-09-19
```

```json
{
  "code": "0",
  "message": "success",
  "data": {
    "keyword": "接口",
    "type": "TASK",
    "items": [
      {
        "type": "TASK",
        "id": 2,
        "title": "实现搜索接口",
        "summary": "完成统一搜索",
        "updatedAt": "2026-09-19T08:00:00Z",
        "projectId": 1,
        "projectName": "研发罗盘",
        "taskId": null
      }
    ]
  }
}
```

## 为什么第一版不用 Elasticsearch

当前是个人工具，项目、任务、日志数量不大。PostgreSQL 的 `ILIKE '%关键字%'` 就能做包含搜索，并和项目、状态、日期筛选放在同一次查询里。

普通 B-Tree 索引帮不上“任意位置包含”这种匹配，所以本任务只为未删除数据的更新时间、日志日期建索引，不为关键字建全文索引。

引入 Elasticsearch 的边界：

- 搜索已经明显变慢，或结果需要按相关度排序、高亮、中文分词；
- 资料量已经不像个人工具，需要把搜索从业务库里拆出去。

在那之前，继续用 PostgreSQL。上 Elasticsearch 还要多跑一个服务，并在项目、任务、日志变更时同步数据，成本高于当前收益。
