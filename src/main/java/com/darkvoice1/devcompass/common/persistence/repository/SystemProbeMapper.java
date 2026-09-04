package com.darkvoice1.devcompass.common.persistence.repository;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.darkvoice1.devcompass.common.persistence.entity.SystemProbe;

/**
 * 系统探针数据访问接口。
 */
@Mapper
public interface SystemProbeMapper extends BaseMapper<SystemProbe> {
}
