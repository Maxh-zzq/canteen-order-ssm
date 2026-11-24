package com.xatu.canteen.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 调整某日菜品的总供应量。
 */
@Data
public class MenuStockDTO {

    @NotNull(message = "总库存不能为空")
    @Min(value = 0, message = "总库存不能为负数")
    private Integer stockTotal;
}
