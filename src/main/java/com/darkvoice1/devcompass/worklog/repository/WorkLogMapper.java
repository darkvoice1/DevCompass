package com.darkvoice1.devcompass.worklog.repository;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.darkvoice1.devcompass.worklog.entity.WorkLog;

/**
 * 工作日志数据访问接口。
 */
@Mapper
public interface WorkLogMapper extends BaseMapper<WorkLog> {
}
