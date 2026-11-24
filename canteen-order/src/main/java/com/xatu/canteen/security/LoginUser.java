package com.xatu.canteen.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 登录用户，放入 SecurityContext，不落库、不序列化到前端。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String role;
    /** 当前 access token 的 jti，登出时写入黑名单 */
    private String jti;
    /** token 过期时间（秒级时间戳） */
    private Long expireAt;
}
