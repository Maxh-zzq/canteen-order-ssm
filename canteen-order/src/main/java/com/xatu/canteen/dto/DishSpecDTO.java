package com.xatu.canteen.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 菜品规格。
 */
@Data
public class DishSpecDTO {

    @NotBlank(message = "规格名称不能为空")
    @Size(max = 16, message = "规格名称不能超过 16 字")
    private String name;

    private BigDecimal priceDiff = BigDecimal.ZERO;
}
