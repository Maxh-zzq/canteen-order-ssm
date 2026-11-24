package com.xatu.canteen.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 购物车汇总。
 */
@Data
public class CartVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<CartItemVO> items = new ArrayList<>();
    private Integer totalQuantity = 0;
    private BigDecimal totalAmount = BigDecimal.ZERO;
}
