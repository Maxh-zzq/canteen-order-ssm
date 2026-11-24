package com.xatu.canteen.service;

import com.xatu.canteen.common.BizException;
import com.xatu.canteen.common.ResultCode;
import com.xatu.canteen.dto.RechargeDTO;
import com.xatu.canteen.dto.UpdateProfileDTO;
import com.xatu.canteen.entity.SysUser;
import com.xatu.canteen.mapper.SysUserMapper;
import com.xatu.canteen.security.SecurityUtils;
import com.xatu.canteen.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * 用户服务：资料、余额、模拟充值。
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper userMapper;

    public UserVO profile() {
        SysUser user = userMapper.selectById(SecurityUtils.currentUserId());
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        return UserVO.from(user);
    }

    public UserVO updateProfile(UpdateProfileDTO dto) {
        SysUser user = userMapper.selectById(SecurityUtils.currentUserId());
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        if (StringUtils.hasText(dto.getRealName())) {
            user.setRealName(dto.getRealName());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }
        userMapper.updateById(user);
        return UserVO.from(user);
    }

    public BigDecimal balance() {
        SysUser user = userMapper.selectById(SecurityUtils.currentUserId());
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        return user.getBalance();
    }

    @Transactional(rollbackFor = Exception.class)
    public BigDecimal recharge(RechargeDTO dto) {
        int rows = userMapper.addBalance(SecurityUtils.currentUserId(), dto.getAmount());
        if (rows == 0) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        SysUser user = userMapper.selectById(SecurityUtils.currentUserId());
        return user.getBalance();
    }
}
