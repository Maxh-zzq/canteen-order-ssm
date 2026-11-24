package com.xatu.canteen.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 提交评价。
 */
@Data
public class ReviewDTO {

    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低 1 分")
    @Max(value = 5, message = "评分最高 5 分")
    private Integer score;

    @Size(max = 300, message = "评价内容不能超过 300 字")
    private String content;

    /** 逗号分隔的图片地址，最多 3 张 */
    @Size(max = 1000, message = "图片过多")
    private String images;
}
