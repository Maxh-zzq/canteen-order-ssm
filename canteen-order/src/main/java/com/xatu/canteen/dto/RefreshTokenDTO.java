package com.xatu.canteen.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 刷新 Token 入参。
 */
@Data
public class RefreshTokenDTO {

    @NotBlank(message = "refreshToken 不能为空")
    private String refreshToken;
}
