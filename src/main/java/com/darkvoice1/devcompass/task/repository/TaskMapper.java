package com.darkvoice1.devcompass.task.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.darkvoice1.devcompass.task.entity.Task;
import com.darkvoice1.devcompass.task.entity.TaskStatus;

import java.time.Instant;

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
