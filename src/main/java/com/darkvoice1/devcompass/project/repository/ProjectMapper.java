package com.darkvoice1.devcompass.project.repository;

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
