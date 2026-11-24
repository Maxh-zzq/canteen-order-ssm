package com.xatu.canteen.controller;

import com.xatu.canteen.common.PageResult;
import com.xatu.canteen.common.RateLimit;
import com.xatu.canteen.common.Result;
import com.xatu.canteen.dto.PlaceOrderDTO;
import com.xatu.canteen.service.OrderService;
import com.xatu.canteen.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 用户订单接口。
 */
@Tag(name = "05. 订单")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "下单（requestId 幂等）")
    @RateLimit(key = "place-order", count = 10, seconds = 60)
    @PostMapping
    public Result<OrderVO> place(@Valid @RequestBody PlaceOrderDTO dto) {
        return Result.ok(orderService.placeOrder(dto));
    }

    @Operation(summary = "我的订单分页")
    @GetMapping
    public Result<PageResult<OrderVO>> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(orderService.listMine(status, pageNum, pageSize));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{orderNo}")
    public Result<OrderVO> detail(@PathVariable String orderNo) {
        return Result.ok(orderService.detail(orderNo));
    }

    @Operation(summary = "取消待支付订单")
    @PostMapping("/{orderNo}/cancel")
    public Result<Void> cancel(@PathVariable String orderNo,
                               @RequestParam(required = false) String reason) {
        orderService.cancel(orderNo, reason);
        return Result.ok();
    }
}
