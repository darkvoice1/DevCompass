package com.darkvoice1.devcompass.activity.repository;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.darkvoice1.devcompass.activity.entity.Activity;

/**
 * 项目动态数据访问接口。
 */
@Mapper
public interface ActivityMapper extends BaseMapper<Activity> {
}
