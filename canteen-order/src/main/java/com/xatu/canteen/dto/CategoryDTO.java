package com.xatu.canteen.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 分类新增/修改。
 */
@Data
public class CategoryDTO {

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 32, message = "分类名称不能超过 32 字")
    private String name;

    @Min(value = 0, message = "排序不能为负数")
    private Integer sort = 0;

    @Min(value = 0, message = "状态只能是 0 或 1")
    @Max(value = 1, message = "状态只能是 0 或 1")
    private Integer status = 1;
}
