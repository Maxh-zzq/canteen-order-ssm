package com.xatu.canteen.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.xatu.canteen.common.UserRole;
import com.xatu.canteen.entity.SysUser;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 用户信息（手机号脱敏）。
 * id 使用字符串返回，避免雪花 ID 在浏览器 Number 中丢失精度。
 */
@Data
public class UserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String roleCode;
    private String roleName;
    private BigDecimal balance;
    private Integer status;

    public static UserVO from(SysUser user) {
        if (user == null) {
            return null;
        }
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setPhone(maskPhone(user.getPhone()));
        vo.setRoleCode(user.getRoleCode());
        vo.setRoleName(UserRole.of(user.getRoleCode()).getDesc());
        vo.setBalance(user.getBalance());
        vo.setStatus(user.getStatus());
        return vo;
    }

    private static String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
