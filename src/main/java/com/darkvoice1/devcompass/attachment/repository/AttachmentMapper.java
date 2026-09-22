package com.darkvoice1.devcompass.attachment.repository;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.darkvoice1.devcompass.attachment.entity.Attachment;

/**
 * 项目附件数据访问接口。
 */
@Mapper
public interface AttachmentMapper extends BaseMapper<Attachment> {
}
