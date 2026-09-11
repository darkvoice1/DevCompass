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

## 设计说明

- `active` 索引使用 PostgreSQL 部分索引，只保存 `deleted_at IS NULL` 的记录，匹配系统普通查询默认排除软删除数据的规则。
- 任务标题关键字使用 `LIKE` 查询。普通 B-Tree 索引无法有效覆盖任意位置的关键字搜索，因此暂不引入全文索引。
- 复合索引的列顺序先放等值筛选字段，再放排序字段，减少常用项目任务列表的扫描范围。
- 分区表和全文索引不在当前任务范围内。
