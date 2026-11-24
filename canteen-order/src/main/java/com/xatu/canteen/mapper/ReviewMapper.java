package com.xatu.canteen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xatu.canteen.entity.Review;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

/**
 * 评价 Mapper。
 */
public interface ReviewMapper extends BaseMapper<Review> {

    @Select("SELECT IFNULL(AVG(r.score), 0) FROM review r "
            + "JOIN order_item oi ON oi.order_id = r.order_id "
            + "WHERE oi.dish_id = #{dishId} AND r.deleted = 0")
    BigDecimal avgScoreOfDish(@Param("dishId") Long dishId);

    @Select("SELECT r.* FROM review r JOIN order_item oi ON oi.order_id = r.order_id "
            + "WHERE oi.dish_id = #{dishId} AND r.deleted = 0 "
            + "ORDER BY r.create_time DESC LIMIT #{limit}")
    List<Review> listByDish(@Param("dishId") Long dishId, @Param("limit") int limit);
}
