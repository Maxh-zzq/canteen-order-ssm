package com.xatu.canteen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xatu.canteen.entity.SysUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 用户 Mapper。余额增减用条件更新，避免并发扣成负数。
 */
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Update("UPDATE sys_user SET balance = balance - #{amount} WHERE id = #{userId} AND balance >= #{amount} AND deleted = 0")
    int deductBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    @Update("UPDATE sys_user SET balance = balance + #{amount} WHERE id = #{userId} AND deleted = 0")
    int addBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
}
