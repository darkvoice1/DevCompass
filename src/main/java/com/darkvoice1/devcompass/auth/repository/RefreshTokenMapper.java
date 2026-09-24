package com.darkvoice1.devcompass.auth.repository;

import java.time.Instant;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.darkvoice1.devcompass.auth.entity.RefreshToken;

/**
 * Refresh Token 数据访问接口。
 */
@Mapper
public interface RefreshTokenMapper extends BaseMapper<RefreshToken> {

    /**
     * 按摘要查询并锁定令牌，避免同一令牌被并发刷新两次。
     *
     * @param tokenHash Refresh Token 摘要
     * @return Refresh Token，不存在时返回空
     */
    @Select("SELECT * FROM refresh_token WHERE token_hash = #{tokenHash} FOR UPDATE")
    RefreshToken selectByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    /**
     * 撤销旧令牌并记录替代它的新令牌。
     *
     * @param id 旧令牌主键
     * @param revokedAt 撤销时间
     * @param replacementId 新令牌主键
     * @return 更新行数
     */
    @Update("UPDATE refresh_token SET revoked_at = #{revokedAt}, "
            + "replaced_by_token_id = #{replacementId}, updated_at = #{revokedAt} "
            + "WHERE id = #{id} AND revoked_at IS NULL")
    int rotate(@Param("id") Long id, @Param("revokedAt") Instant revokedAt,
            @Param("replacementId") Long replacementId);
}
