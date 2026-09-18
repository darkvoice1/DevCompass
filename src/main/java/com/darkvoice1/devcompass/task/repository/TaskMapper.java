package com.darkvoice1.devcompass.task.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.darkvoice1.devcompass.dashboard.dto.FocusListItemResponse;
import com.darkvoice1.devcompass.task.entity.Task;
import com.darkvoice1.devcompass.task.entity.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * 任务数据访问接口。
 */
@Mapper
public interface TaskMapper extends BaseMapper<Task> {

    /**
     * 查询指定日期范围内未完成的焦点任务。
     *
     * @param fromDate 截止日期起始值，包含当天
     * @param toDate 截止日期结束值，包含当天
     * @return 可跳转到项目和任务详情的清单项
     */
    @Select("""
            <script>
            SELECT t.id AS task_id, t.title, t.status, t.due_date,
                   t.project_id, p.name AS project_name, 'TASK' AS item_kind
            FROM task t
            INNER JOIN project p ON p.id = t.project_id
            WHERE t.deleted_at IS NULL
              AND p.deleted_at IS NULL
              AND p.archived = FALSE
              AND t.status NOT IN ('COMPLETED', 'CANCELLED')
              AND t.due_date IS NOT NULL
              AND t.due_date &lt;= #{toDate}
              <if test="fromDate != null">
                AND t.due_date &gt;= #{fromDate}
              </if>
            ORDER BY t.due_date ASC, t.id ASC
            </script>
            """)
    List<FocusListItemResponse> selectFocusListTasks(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * 查询指定的已软删除任务。
     */
    @Select("SELECT * FROM task WHERE id = #{id} AND deleted_at IS NOT NULL")
    Task selectDeletedById(@Param("id") Long id);

    /**
     * 将任务标记为软删除。
     */
    @Update("UPDATE task SET deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP "
            + "WHERE id = #{id} AND deleted_at IS NULL")
    int softDeleteById(@Param("id") Long id);

    /**
     * 恢复已软删除任务。
     */
    @Update("UPDATE task SET deleted_at = NULL, updated_at = CURRENT_TIMESTAMP "
            + "WHERE id = #{id} AND deleted_at IS NOT NULL")
    int restoreById(@Param("id") Long id);

    /**
     * 仅在数据库中的状态仍为预期状态时更新任务状态。
     *
     * @param id 任务主键
     * @param currentStatus 变更前的状态
     * @param targetStatus 变更后的状态
     * @param updatedAt 更新时间
     * @return 实际更新的记录数
     */
    @Update("UPDATE task SET status = #{targetStatus}, updated_at = #{updatedAt} "
            + "WHERE id = #{id} AND status = #{currentStatus} AND deleted_at IS NULL")
    int updateStatusIfCurrent(@Param("id") Long id,
            @Param("currentStatus") TaskStatus currentStatus,
            @Param("targetStatus") TaskStatus targetStatus,
            @Param("updatedAt") Instant updatedAt);
}
