package com.xatu.canteen.controller;

import com.xatu.canteen.common.OperLog;
import com.xatu.canteen.common.Result;
import com.xatu.canteen.dto.PayCallbackDTO;
import com.xatu.canteen.dto.RefundDTO;
import com.xatu.canteen.service.PaymentService;
import com.xatu.canteen.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 支付接口。
 */
@Tag(name = "07. 支付")
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "余额支付")
    @PostMapping("/balance/{orderNo}")
    public Result<OrderVO> payByBalance(@PathVariable String orderNo) {
        return Result.ok(paymentService.payByBalance(orderNo));
    }

    @Operation(summary = "模拟支付网关回调（验签 + 幂等）")
    @PostMapping("/mock/callback")
    public Result<String> mockCallback(@Valid @RequestBody PayCallbackDTO dto) {
        return Result.ok(paymentService.mockCallback(dto));
    }

    @Operation(summary = "管理员退款（退回余额并回滚库存）")
    @PreAuthorize("hasRole('ADMIN')")
    @OperLog(module = "支付管理", action = "订单退款")
    @PostMapping("/{orderNo}/refund")
    public Result<Void> refund(@PathVariable String orderNo,
                               @RequestBody(required = false) RefundDTO dto) {
        paymentService.refund(orderNo, dto == null ? null : dto.getReason());
        return Result.ok();
    }
}
