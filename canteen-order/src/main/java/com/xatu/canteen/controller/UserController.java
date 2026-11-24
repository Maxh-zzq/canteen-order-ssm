package com.xatu.canteen.controller;

import com.xatu.canteen.common.Result;
import com.xatu.canteen.dto.RechargeDTO;
import com.xatu.canteen.dto.UpdateProfileDTO;
import com.xatu.canteen.service.UserService;
import com.xatu.canteen.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.math.BigDecimal;

/**
 * 个人中心接口。
 */
@Tag(name = "02. 用户")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "当前用户信息")
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.ok(userService.profile());
    }

    @Operation(summary = "修改姓名/手机号")
    @PutMapping("/me")
    public Result<UserVO> update(@Valid @RequestBody UpdateProfileDTO dto) {
        return Result.ok(userService.updateProfile(dto));
    }

    @Operation(summary = "查询余额")
    @GetMapping("/me/balance")
    public Result<BigDecimal> balance() {
        return Result.ok(userService.balance());
    }

    @Operation(summary = "模拟充值（单次上限 500）")
    @PostMapping("/me/recharge")
    public Result<BigDecimal> recharge(@Valid @RequestBody RechargeDTO dto) {
        return Result.ok(userService.recharge(dto));
    }
}
