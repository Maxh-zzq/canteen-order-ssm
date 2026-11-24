package com.xatu.canteen.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录 / 刷新结果。
 */
@Data
public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    /** access token 有效期，秒 */
    private long expiresIn;
    private UserVO user;
}
