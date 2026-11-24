package com.xatu.canteen.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 下单入参。requestId 由前端生成（UUID），用于接口幂等。
 */
@Data
public class PlaceOrderDTO {

    @NotBlank(message = "requestId 不能为空")
    @Size(max = 64, message = "requestId 过长")
    private String requestId;

    /** 期望取餐时间，可为空 */
    private LocalDateTime expectTime;

    @Size(max = 100, message = "备注不能超过 100 字")
    private String remark;
}
