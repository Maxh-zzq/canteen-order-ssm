package com.xatu.canteen.dto;

import lombok.Data;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 修改个人资料。
 */
@Data
public class UpdateProfileDTO {

    @Size(max = 32, message = "姓名长度不能超过 32")
    private String realName;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
