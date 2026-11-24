package com.xatu.canteen.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单条目快照。
 */
@Data
public class OrderItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long dishId;
    private String dishName;
    private String specName;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal amount;
}
