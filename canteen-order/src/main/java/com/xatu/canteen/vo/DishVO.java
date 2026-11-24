package com.xatu.canteen.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 菜品详情。
 */
@Data
public class DishVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long stallId;
    private String stallName;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String coverUrl;
    private BigDecimal price;
    private String description;
    private Integer dailyStock;
    private Integer sales;
    private BigDecimal avgScore;
    private Integer status;
    private List<DishSpecVO> specs;
}
