package com.xatu.canteen.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 购物车条目。
 */
@Data
public class CartItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long dishId;
    private Long specId;
    private String specName;
    private String dishName;
    private String coverUrl;
    /** 含规格差价的单价 */
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal amount;
    private Integer stockLeft;
}
