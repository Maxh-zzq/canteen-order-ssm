package com.xatu.canteen.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 档口新增/修改。
 */
@Data
public class StallDTO {

    @NotBlank(message = "档口名称不能为空")
    @Size(max = 32, message = "档口名称不能超过 32 字")
    private String name;

    @Size(max = 8, message = "楼层过长")
    private String floor;

    @Size(max = 8, message = "窗口号过长")
    private String windowNo;

    private Long managerId;

    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "开始时间格式应为 HH:mm")
    private String openTime;

    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "结束时间格式应为 HH:mm")
    private String closeTime;

    @Min(value = 0, message = "状态只能是 0 或 1")
    @Max(value = 1, message = "状态只能是 0 或 1")
    private Integer status = 1;
}
