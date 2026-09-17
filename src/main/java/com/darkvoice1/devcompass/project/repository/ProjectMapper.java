package com.darkvoice1.devcompass.project.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.darkvoice1.devcompass.project.entity.Project;

/**
 * 项目数据访问接口。
 */
@Mapper
public interface ProjectMapper extends BaseMapper<Project> {

    /**
     * 查询仪表盘需要展示的未归档项目，并按最近更新时间排序。
     *
     * @return 仪表盘项目列表
     */
    @Select("""
            SELECT id, name, status, progress_mode, auto_progress, manual_progress, tags, updated_at
            FROM project
            WHERE archived = FALSE AND deleted_at IS NULL
            ORDER BY updated_at DESC, id DESC
            """)
    List<Project> selectDashboardProjects();

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
