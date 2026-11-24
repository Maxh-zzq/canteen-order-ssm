package com.xatu.canteen.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 参数配置新增/修改。
 */
@Data
public class ConfigDTO {

    @NotBlank(message = "配置键不能为空")
    @Size(max = 64, message = "配置键不能超过 64 字")
    private String configKey;

    @NotBlank(message = "配置值不能为空")
    @Size(max = 256, message = "配置值不能超过 256 字")
    private String configValue;

    @Size(max = 128, message = "备注不能超过 128 字")
    private String remark;
}
