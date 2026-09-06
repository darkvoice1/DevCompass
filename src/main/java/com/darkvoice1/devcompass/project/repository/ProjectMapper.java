package com.darkvoice1.devcompass.project.repository;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.darkvoice1.devcompass.project.entity.Project;

/**
 * 项目数据访问接口。
 */
@Mapper
public interface ProjectMapper extends BaseMapper<Project> {
}
