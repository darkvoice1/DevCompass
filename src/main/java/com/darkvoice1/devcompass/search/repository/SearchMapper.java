package com.darkvoice1.devcompass.search.repository;

import java.time.Instant;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.darkvoice1.devcompass.project.entity.ProjectStatus;
import com.darkvoice1.devcompass.search.dto.SearchItemResponse;
import com.darkvoice1.devcompass.task.entity.TaskStatus;

/**
 * 全局搜索数据访问接口。
 */
@Mapper
public interface SearchMapper {

    /**
     * 按可选条件搜索未归档、未删除项目。
     *
     * @param keyword 已转义并带通配符的匹配模式，可为空
     * @param projectId 项目主键，可为空
     * @param status 项目状态，可为空
     * @param updatedFrom 更新时间起点，可为空
     * @param updatedTo 更新时间终点（不含），可为空
     * @return 匹配的项目结果
     */
    @Select("""
            <script>
            SELECT p.id, 'PROJECT' AS type, p.name AS title, p.description AS summary,
                   p.updated_at, p.id AS project_id, p.name AS project_name
            FROM project p
            WHERE p.deleted_at IS NULL
              AND p.archived = FALSE
              <if test="keyword != null">
                AND (
                      p.name ILIKE #{keyword} ESCAPE '\\'
                      OR p.description ILIKE #{keyword} ESCAPE '\\'
                      OR p.tags ILIKE #{keyword} ESCAPE '\\'
                    )
              </if>
              <if test="projectId != null">
                AND p.id = #{projectId}
              </if>
              <if test="status != null">
                AND p.status = #{status}
              </if>
              <if test="updatedFrom != null">
                AND p.updated_at &gt;= #{updatedFrom}
              </if>
              <if test="updatedTo != null">
                AND p.updated_at &lt; #{updatedTo}
              </if>
            ORDER BY p.updated_at DESC, p.id DESC
            </script>
            """)
    List<SearchItemResponse> selectProjects(
            @Param("keyword") String keyword,
            @Param("projectId") Long projectId,
            @Param("status") ProjectStatus status,
            @Param("updatedFrom") Instant updatedFrom,
            @Param("updatedTo") Instant updatedTo);

    /**
     * 按可选条件搜索未删除任务，所属项目必须未归档、未删除。
     *
     * @param keyword 已转义并带通配符的匹配模式，可为空
     * @param projectId 项目主键，可为空
     * @param status 任务状态，可为空
     * @param updatedFrom 更新时间起点，可为空
     * @param updatedTo 更新时间终点（不含），可为空
     * @return 匹配的任务结果
     */
    @Select("""
            <script>
            SELECT t.id, 'TASK' AS type, t.title, t.description AS summary,
                   t.updated_at, t.project_id, p.name AS project_name
            FROM task t
            INNER JOIN project p ON p.id = t.project_id
            WHERE t.deleted_at IS NULL
              AND p.deleted_at IS NULL
              AND p.archived = FALSE
              <if test="keyword != null">
                AND (
                      t.title ILIKE #{keyword} ESCAPE '\\'
                      OR t.description ILIKE #{keyword} ESCAPE '\\'
                    )
              </if>
              <if test="projectId != null">
                AND t.project_id = #{projectId}
              </if>
              <if test="status != null">
                AND t.status = #{status}
              </if>
              <if test="updatedFrom != null">
                AND t.updated_at &gt;= #{updatedFrom}
              </if>
              <if test="updatedTo != null">
                AND t.updated_at &lt; #{updatedTo}
              </if>
            ORDER BY t.updated_at DESC, t.id DESC
            </script>
            """)
    List<SearchItemResponse> selectTasks(
            @Param("keyword") String keyword,
            @Param("projectId") Long projectId,
            @Param("status") TaskStatus status,
            @Param("updatedFrom") Instant updatedFrom,
            @Param("updatedTo") Instant updatedTo);
}
