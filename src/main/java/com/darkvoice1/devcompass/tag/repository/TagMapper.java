package com.darkvoice1.devcompass.tag.repository;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.darkvoice1.devcompass.tag.entity.Tag;

/**
 * 标签数据访问接口。
 */
@Mapper
public interface TagMapper extends BaseMapper<Tag> {
}
