package com.xatu.canteen.dto;

import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 退款入参。
 */
@Data
public class RefundDTO {

    @Size(max = 100, message = "退款原因不能超过 100 字")
    private String reason;
}
