package com.darkvoice1.devcompass.project.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.darkvoice1.devcompass.project.entity.ProjectPhase;

/**
 * 项目阶段数据访问接口。
 */
@Mapper
public interface ProjectPhaseMapper extends BaseMapper<ProjectPhase> {

    /**
     * 查询指定的已软删除阶段。
     */
    @Select("SELECT * FROM project_phase WHERE id = #{id} AND deleted_at IS NOT NULL")
    ProjectPhase selectDeletedById(@Param("id") Long id);

    /**
     * 将项目阶段标记为软删除。
     */
    @Update("UPDATE project_phase SET deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP "
            + "WHERE id = #{id} AND deleted_at IS NULL")
    int softDeleteById(@Param("id") Long id);

    /**
     * 恢复已软删除项目阶段。
     */
    @Update("UPDATE project_phase SET deleted_at = NULL, updated_at = CURRENT_TIMESTAMP "
            + "WHERE id = #{id} AND deleted_at IS NOT NULL")
    int restoreById(@Param("id") Long id);
}
