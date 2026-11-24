package com.xatu.canteen.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * 菜品新增/修改。
 */
@Data
public class DishDTO {

    @NotNull(message = "档口不能为空")
    private Long stallId;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    @NotBlank(message = "菜品名称不能为空")
    @Size(max = 64, message = "菜品名称不能超过 64 字")
    private String name;

    @Size(max = 256, message = "图片地址过长")
    private String coverUrl;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于 0")
    private BigDecimal price;

    @Size(max = 500, message = "描述不能超过 500 字")
    private String description;

    @Min(value = 0, message = "每日库存不能为负数")
    private Integer dailyStock = 50;

    @Min(value = 0, message = "状态只能是 0 或 1")
    @Max(value = 1, message = "状态只能是 0 或 1")
    private Integer status = 1;

    @Valid
    private List<DishSpecDTO> specs;
}
