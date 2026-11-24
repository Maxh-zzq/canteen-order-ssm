package com.xatu.canteen.controller;

import com.xatu.canteen.common.RateLimit;
import com.xatu.canteen.common.Result;
import com.xatu.canteen.dto.LoginDTO;
import com.xatu.canteen.dto.RefreshTokenDTO;
import com.xatu.canteen.security.SecurityUtils;
import com.xatu.canteen.service.AuthService;
import com.xatu.canteen.service.CaptchaService;
import com.xatu.canteen.vo.CaptchaVO;
import com.xatu.canteen.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 认证接口。
 */
@Tag(name = "01. 认证")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;

    @Operation(summary = "获取图形验证码（默认关闭时可不调用）")
    @GetMapping("/captcha")
    public Result<CaptchaVO> captcha() {
        return Result.ok(captchaService.generate());
    }

    @Operation(summary = "登录，返回双 Token")
    @RateLimit(key = "login", count = 10, seconds = 60)
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok(authService.login(dto));
    }

    @Operation(summary = "刷新 Token（Refresh 轮换）")
    @PostMapping("/refresh")
    public Result<LoginVO> refresh(@Valid @RequestBody RefreshTokenDTO dto) {
        return Result.ok(authService.refresh(dto.getRefreshToken()));
    }

    @Operation(summary = "登出，当前 Token 加入黑名单")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout(SecurityUtils.currentUser());
        return Result.ok();
    }
}
