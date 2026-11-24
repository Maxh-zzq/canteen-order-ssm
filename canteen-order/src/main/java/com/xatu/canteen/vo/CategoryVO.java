package com.xatu.canteen.vo;

import com.xatu.canteen.entity.DishCategory;
import lombok.Data;

import java.io.Serializable;

/**
 * 菜品分类。
 */
@Data
public class CategoryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private Integer sort;
    private Integer status;

    public static CategoryVO from(DishCategory category) {
        if (category == null) {
            return null;
        }
        CategoryVO vo = new CategoryVO();
        vo.setId(category.getId());
        vo.setName(category.getName());
        vo.setSort(category.getSort());
        vo.setStatus(category.getStatus());
        return vo;
    }
}
