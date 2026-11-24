package com.xatu.canteen.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 加入购物车。
 */
@Data
public class CartAddDTO {

    @NotNull(message = "菜品不能为空")
    private Long dishId;

    /** 无规格传 0 或为空 */
    private Long specId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为 1")
    @Max(value = 20, message = "单个菜品最多购买 20 份")
    private Integer quantity;
}
