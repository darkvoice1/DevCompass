package com.darkvoice1.devcompass.task.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.darkvoice1.devcompass.task.entity.Task;

/**
 * 任务数据访问接口。
 */
@Mapper
public interface TaskMapper extends BaseMapper<Task> {

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
}
