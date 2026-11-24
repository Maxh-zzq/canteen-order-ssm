package com.xatu.canteen.vo;

import com.xatu.canteen.entity.DishSpec;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 菜品规格。
 */
@Data
public class DishSpecVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private BigDecimal priceDiff;

    public static DishSpecVO from(DishSpec spec) {
        if (spec == null) {
            return null;
        }
        DishSpecVO vo = new DishSpecVO();
        vo.setId(spec.getId());
        vo.setName(spec.getName());
        vo.setPriceDiff(spec.getPriceDiff());
        return vo;
    }
}
