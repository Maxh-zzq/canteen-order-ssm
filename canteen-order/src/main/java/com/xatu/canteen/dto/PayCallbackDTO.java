package com.xatu.canteen.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 模拟支付网关回调。sign = MD5(orderNo + tradeNo + amount + secret)。
 */
@Data
public class PayCallbackDTO {

    @NotBlank(message = "orderNo 不能为空")
    private String orderNo;

    @NotBlank(message = "tradeNo 不能为空")
    private String tradeNo;

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.01", message = "金额必须大于 0")
    private BigDecimal amount;

    @NotBlank(message = "签名不能为空")
    private String sign;
}
