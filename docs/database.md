# 数据库索引说明

项目使用 PostgreSQL 和 Flyway 管理数据库结构。索引迁移脚本只增加索引，不改变业务数据。

## 核心索引

| 索引 | 表 | 作用 |
| --- | --- | --- |
| `idx_project_archived_status` | `project` | 按归档状态和项目状态筛选项目。 |
| `idx_project_active_status` | `project` | 普通项目列表只查询未软删除数据，按状态和更新时间筛选。 |
| `idx_project_phase_project_sort_order` | `project_phase` | 查询项目阶段并按阶段顺序排列。 |
| `idx_project_phase_active_project_sort` | `project_phase` | 查询未软删除阶段，并按项目、排序序号和 ID 稳定排序。 |
| `idx_task_project_status` | `task` | 按项目和任务状态筛选任务。 |
| `idx_task_project_priority` | `task` | 按项目和任务优先级筛选任务。 |
| `idx_task_project_due_date` | `task` | 按项目和截止日期排序任务。 |
| `idx_task_project_phase` | `task` | 按项目和阶段筛选任务。 |
| `idx_task_active_project_status_priority_due` | `task` | 覆盖未软删除任务按项目、状态、优先级筛选，并按截止日期和更新时间排序。 |
| `idx_task_active_project_phase_due` | `task` | 覆盖未软删除任务按项目、阶段筛选，并按截止日期和更新时间排序。 |
| `idx_project_active_unarchived_status` | `project` | 仪表盘只查询未归档、未软删除项目，并按状态筛选。 |
| `idx_task_active_project_updated` | `task` | 按项目聚合未软删除任务的最近更新时间，供仪表盘计算活跃度。 |
| `idx_work_log_active_task_updated` | `work_log` | 按任务聚合未软删除工作日志的最近更新时间，供仪表盘计算活跃度。 |
| `idx_task_active_due_date` | `task` | 首页按截止日期范围查询未完成、未删除任务。 |
| `idx_task_active_blocked_updated` | `task` | 首页查询未完成的阻塞任务，并按更新时间排序。 |
| `idx_project_active_unarchived_target_date` | `project` | 首页查询目标日期已过的未完成、未归档项目。 |

## 设计说明

- `active` 索引使用 PostgreSQL 部分索引，只保存 `deleted_at IS NULL` 的记录，匹配系统普通查询默认排除软删除数据的规则。
- 仪表盘项目索引额外限制 `archived = FALSE`，因为首页聚合默认不展示已归档项目。
- 焦点清单按截止日期或阻塞标记跨项目查询，因此相关索引不以 `project_id` 开头，并用部分索引排除已完成、已取消和已删除数据。
- 任务标题关键字使用 `LIKE` 查询。普通 B-Tree 索引无法有效覆盖任意位置的关键字搜索，因此暂不引入全文索引。
- 项目标签保存在逗号分隔字符串中，仪表盘按完整标签匹配，暂不引入 GIN 或全文索引。
- 复合索引的列顺序先放等值筛选字段，再放排序字段，减少常用项目任务列表的扫描范围。
- 分区表和全文索引不在当前任务范围内。
