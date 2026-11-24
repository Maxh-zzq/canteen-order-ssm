package com.xatu.canteen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xatu.canteen.entity.Dish;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 菜品 Mapper。
 */
public interface DishMapper extends BaseMapper<Dish> {

    @Update("UPDATE dish SET sales = sales + #{qty} WHERE id = #{id} AND deleted = 0")
    int increaseSales(@Param("id") Long id, @Param("qty") int qty);

    @Update("UPDATE dish SET sales = GREATEST(sales - #{qty}, 0) WHERE id = #{id} AND deleted = 0")
    int decreaseSales(@Param("id") Long id, @Param("qty") int qty);

    @Update("UPDATE dish SET avg_score = #{score} WHERE id = #{id} AND deleted = 0")
    int updateAvgScore(@Param("id") Long id, @Param("score") BigDecimal score);
}
