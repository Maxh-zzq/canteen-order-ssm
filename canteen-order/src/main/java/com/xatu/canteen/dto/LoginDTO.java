package com.xatu.canteen.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 登录入参。
 */
@Data
public class LoginDTO {

    @NotBlank(message = "账号不能为空")
    @Size(max = 32, message = "账号长度不能超过 32")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(max = 64, message = "密码长度不能超过 64")
    private String password;

    /** 验证码开启时必填 */
    private String captchaKey;
    private String captchaCode;
}
