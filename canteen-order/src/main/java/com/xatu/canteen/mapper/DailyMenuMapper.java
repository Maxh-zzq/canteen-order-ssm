package com.xatu.canteen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xatu.canteen.entity.DailyMenu;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 每日菜单 Mapper。库存扣减与回滚都是条件更新，DB 是最终一致性依据。
 */
public interface DailyMenuMapper extends BaseMapper<DailyMenu> {

    @Update("UPDATE daily_menu SET stock_left = stock_left - #{qty}, sales = sales + #{qty} "
            + "WHERE id = #{id} AND stock_left >= #{qty} AND deleted = 0")
    int deductStock(@Param("id") Long id, @Param("qty") int qty);

    @Update("UPDATE daily_menu SET stock_left = LEAST(stock_left + #{qty}, stock_total), "
            + "sales = GREATEST(sales - #{qty}, 0) WHERE id = #{id} AND deleted = 0")
    int restoreStock(@Param("id") Long id, @Param("qty") int qty);
}
