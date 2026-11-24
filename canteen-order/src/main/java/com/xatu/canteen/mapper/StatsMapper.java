package com.xatu.canteen.mapper;

import com.xatu.canteen.vo.SalesTrendVO;
import com.xatu.canteen.vo.StatsOverviewVO;
import com.xatu.canteen.vo.TopDishVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 统计报表 Mapper。只统计支付成功及之后状态的订单，退款/取消不计入营收。
 */
public interface StatsMapper {

    @Select("SELECT "
            + "(SELECT COUNT(*) FROM order_info WHERE deleted = 0 AND status IN (20,30,40,50) AND DATE(pay_time) = CURDATE()) AS today_order_count, "
            + "(SELECT IFNULL(SUM(pay_amount), 0) FROM order_info WHERE deleted = 0 AND status IN (20,30,40,50) AND DATE(pay_time) = CURDATE()) AS today_revenue, "
            + "(SELECT COUNT(*) FROM sys_user WHERE deleted = 0) AS total_users, "
            + "(SELECT IFNULL(AVG(score), 0) FROM review WHERE deleted = 0) AS avg_score, "
            + "(SELECT COUNT(*) FROM order_info WHERE deleted = 0 AND status = 10) AS pending_orders")
    StatsOverviewVO overview();

    @Select("SELECT DATE_FORMAT(pay_time, '%Y-%m-%d') AS stat_date, COUNT(*) AS order_count, "
            + "IFNULL(SUM(pay_amount), 0) AS revenue FROM order_info "
            + "WHERE deleted = 0 AND status IN (20,30,40,50) AND pay_time >= #{start} "
            + "GROUP BY stat_date ORDER BY stat_date")
    List<SalesTrendVO> salesTrend(@Param("start") LocalDateTime start);

    @Select("SELECT oi.dish_name AS dish_name, SUM(oi.quantity) AS quantity, SUM(oi.amount) AS amount "
            + "FROM order_item oi JOIN order_info o ON o.id = oi.order_id "
            + "WHERE o.deleted = 0 AND o.status IN (20,30,40,50) AND o.pay_time >= #{start} "
            + "GROUP BY oi.dish_id, oi.dish_name ORDER BY quantity DESC LIMIT #{limit}")
    List<TopDishVO> topDishes(@Param("start") LocalDateTime start, @Param("limit") int limit);
}
