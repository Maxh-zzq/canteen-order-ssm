package com.xatu.canteen.controller;

import com.xatu.canteen.common.PageResult;
import com.xatu.canteen.common.Result;
import com.xatu.canteen.service.OrderService;
import com.xatu.canteen.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 档口员工出餐接口。
 */
@Tag(name = "06. 档口出餐")
@RestController
@RequestMapping("/api/v1/staff/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STAFF','ADMIN')")
public class StaffController {

    private final OrderService orderService;

    @Operation(summary = "档口订单列表（默认已支付/备餐中/可取餐）")
    @GetMapping
    public Result<PageResult<OrderVO>> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(orderService.staffList(status, pageNum, pageSize));
    }

    @Operation(summary = "接单：已支付 -> 备餐中")
    @PostMapping("/{orderNo}/accept")
    public Result<Void> accept(@PathVariable String orderNo) {
        orderService.accept(orderNo);
        return Result.ok();
    }

    @Operation(summary = "出餐：备餐中 -> 可取餐")
    @PostMapping("/{orderNo}/ready")
    public Result<Void> ready(@PathVariable String orderNo) {
        orderService.ready(orderNo);
        return Result.ok();
    }

    @Operation(summary = "取餐核销：校验取餐码")
    @PostMapping("/{orderNo}/pickup")
    public Result<Void> pickup(@PathVariable String orderNo,
                               @RequestParam String pickupCode) {
        orderService.pickup(orderNo, pickupCode);
        return Result.ok();
    }
}
