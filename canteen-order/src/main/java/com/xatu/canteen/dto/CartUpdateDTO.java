package com.xatu.canteen.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 修改购物车数量，quantity 为 0 表示删除该条目。
 */
@Data
public class CartUpdateDTO {

    @NotNull(message = "菜品不能为空")
    private Long dishId;

    private Long specId;

    @NotNull(message = "数量不能为空")
    @Min(value = 0, message = "数量不能为负数")
    @Max(value = 20, message = "单个菜品最多购买 20 份")
    private Integer quantity;
}
