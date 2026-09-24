package com.darkvoice1.devcompass.auth.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.darkvoice1.devcompass.auth.entity.UserAccount;

/**
 * 用户账号数据访问接口。
 */
@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {

    /**
     * 不区分大小写查询用户名。
     *
     * @param username 已规范化的用户名
     * @return 用户账号，不存在时返回空
     */
    @Select("SELECT * FROM user_account WHERE LOWER(username) = LOWER(#{username})")
    UserAccount selectByUsername(@Param("username") String username);

    /**
     * 查询当前用户总数。
     *
     * @return 用户数量
     */
    @Select("SELECT COUNT(*) FROM user_account")
    long countAll();
}
