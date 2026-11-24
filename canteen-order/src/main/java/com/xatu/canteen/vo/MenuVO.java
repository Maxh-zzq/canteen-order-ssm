package com.xatu.canteen.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 今日菜单条目（菜品 + 当日库存）。
 */
@Data
public class MenuVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long menuId;
    private Long dishId;
    private String dishName;
    private String coverUrl;
    private BigDecimal price;
    private Long categoryId;
    private String categoryName;
    private Long stallId;
    private String stallName;
    private Integer stockTotal;
    private Integer stockLeft;
    private Integer sales;
    private LocalDate serveDate;
    private Integer status;
}
