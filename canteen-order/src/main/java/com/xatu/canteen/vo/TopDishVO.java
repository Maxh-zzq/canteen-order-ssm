package com.xatu.canteen.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 热销菜品。
 */
@Data
public class TopDishVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String dishName;
    private Long quantity;
    private BigDecimal amount;
}
