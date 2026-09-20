package com.darkvoice1.devcompass.timeline.repository;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.darkvoice1.devcompass.timeline.dto.TimelineEventResponse;

/**
 * 时间线数据访问接口。
 */
@Mapper
public interface TimelineMapper {

    /**
     * 按截止日期范围查询未删除任务，所属项目必须未归档、未删除。
     *
     * @param fromDate 截止日期起始值，包含当天
     * @param toDate 截止日期结束值，包含当天
     * @return 时间线任务事件
     */
    @Select("""
            <script>
            SELECT t.id, 'TASK' AS type, t.title, t.due_date AS date,
                   t.project_id, p.name AS project_name, t.status, t.priority
            FROM task t
            INNER JOIN project p ON p.id = t.project_id
            WHERE t.deleted_at IS NULL
              AND p.deleted_at IS NULL
              AND p.archived = FALSE
              AND t.status != 'CANCELLED'
              AND t.due_date IS NOT NULL
              AND t.due_date &gt;= #{fromDate}
              AND t.due_date &lt;= #{toDate}
            ORDER BY t.due_date ASC, t.id ASC
            </script>
            """)
    List<TimelineEventResponse> selectTaskEvents(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);
}
