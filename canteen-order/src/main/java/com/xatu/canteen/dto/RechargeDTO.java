package com.xatu.canteen.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 模拟充值。单次上限 500 元，防止随意刷余额。
 */
@Data
public class RechargeDTO {

    @NotNull(message = "充值金额不能为空")
    @DecimalMin(value = "0.01", message = "充值金额必须大于 0")
    @DecimalMax(value = "500.00", message = "单次充值不能超过 500 元")
    private BigDecimal amount;
}
