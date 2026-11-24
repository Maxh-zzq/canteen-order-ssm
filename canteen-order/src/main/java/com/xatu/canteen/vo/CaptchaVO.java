package com.xatu.canteen.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 图形验证码。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 登录时回传 */
    private String key;
    /** data:image/png;base64,xxx */
    private String image;
}
