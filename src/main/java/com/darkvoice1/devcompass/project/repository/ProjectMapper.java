package com.darkvoice1.devcompass.project.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.darkvoice1.devcompass.project.entity.Project;
import com.darkvoice1.devcompass.project.entity.ProjectStatus;

/**
 * 项目数据访问接口。
 */
@Mapper
public interface ProjectMapper extends BaseMapper<Project> {

    /**
     * 按可选条件查询仪表盘项目，并按最近活跃时间排序。
     *
     * @param status 项目状态，可为空
     * @param tag 完整项目标签，可为空
     * @param activeWithinDays 最近活跃天数，可为空
     * @return 仪表盘项目列表
     */
    @Select("""
            <script>
            WITH filtered_project AS (
                SELECT p.id, p.name, p.status, p.progress_mode, p.auto_progress,
                       p.manual_progress, p.tags, p.updated_at
                FROM project p
                WHERE p.archived = FALSE AND p.deleted_at IS NULL
                <if test="status != null">
                    AND p.status = #{status}
                </if>
                <if test="tag != null and tag != ''">
                    AND p.tags IS NOT NULL
                    AND EXISTS (
                        SELECT 1
                        FROM unnest(string_to_array(p.tags, ',')) AS project_tags(project_tag)
                        WHERE LOWER(BTRIM(project_tag)) = LOWER(#{tag})
                    )
                </if>
            ),
            project_activity AS (
                SELECT t.project_id,
                       MAX(t.updated_at) AS task_updated_at,
                       MAX(w.updated_at) AS work_log_updated_at
                FROM task t
                INNER JOIN filtered_project p ON p.id = t.project_id
                LEFT JOIN work_log w ON w.task_id = t.id AND w.deleted_at IS NULL
                WHERE t.deleted_at IS NULL
                GROUP BY t.project_id
            ),
            dashboard_project AS (
                SELECT p.id, p.name, p.status, p.progress_mode, p.auto_progress,
                       p.manual_progress, p.tags,
                       GREATEST(
                           p.updated_at,
                           COALESCE(a.task_updated_at, p.updated_at),
                           COALESCE(a.work_log_updated_at, p.updated_at)
                       ) AS updated_at
                FROM filtered_project p
                LEFT JOIN project_activity a ON a.project_id = p.id
            )
            SELECT id, name, status, progress_mode, auto_progress, manual_progress, tags, updated_at
            FROM dashboard_project
            <if test="activeWithinDays != null">
                WHERE updated_at >= CURRENT_TIMESTAMP - (#{activeWithinDays} * INTERVAL '1 day')
            </if>
            ORDER BY updated_at DESC, id DESC
            </script>
            """)
    List<Project> selectDashboardProjects(
            @Param("status") ProjectStatus status,
            @Param("tag") String tag,
            @Param("activeWithinDays") Integer activeWithinDays);

    /**
     * 查询指定的已软删除项目。
     */
    @Select("SELECT * FROM project WHERE id = #{id} AND deleted_at IS NOT NULL")
    Project selectDeletedById(@Param("id") Long id);

    /**
     * 将项目标记为软删除。
     */
    @Update("UPDATE project SET deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP "
            + "WHERE id = #{id} AND deleted_at IS NULL")
    int softDeleteById(@Param("id") Long id);

    /**
     * 恢复已软删除项目。
     */
    @Update("UPDATE project SET deleted_at = NULL, updated_at = CURRENT_TIMESTAMP "
            + "WHERE id = #{id} AND deleted_at IS NOT NULL")
    int restoreById(@Param("id") Long id);
}
