package com.xatu.canteen.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 销售趋势（按天）。
 */
@Data
public class SalesTrendVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String statDate;
    private Long orderCount;
    private BigDecimal revenue;
}
