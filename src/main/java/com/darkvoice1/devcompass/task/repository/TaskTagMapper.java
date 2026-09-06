package com.darkvoice1.devcompass.task.repository;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.darkvoice1.devcompass.task.entity.TaskTag;

/**
 * 任务标签关联数据访问接口。
 */
@Mapper
public interface TaskTagMapper extends BaseMapper<TaskTag> {
}
