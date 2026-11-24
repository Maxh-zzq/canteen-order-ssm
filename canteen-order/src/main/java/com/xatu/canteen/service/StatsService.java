package com.xatu.canteen.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xatu.canteen.common.OrderStatus;
import com.xatu.canteen.entity.OrderInfo;
import com.xatu.canteen.mapper.OrderInfoMapper;
import com.xatu.canteen.mapper.StatsMapper;
import com.xatu.canteen.vo.SalesTrendVO;
import com.xatu.canteen.vo.StatsOverviewVO;
import com.xatu.canteen.vo.TopDishVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * 统计报表与订单导出。
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<Integer> PAID_STATUS = Arrays.asList(
            OrderStatus.PAID.getCode(), OrderStatus.PREPARING.getCode(),
            OrderStatus.READY.getCode(), OrderStatus.COMPLETED.getCode());

    private final StatsMapper statsMapper;
    private final OrderInfoMapper orderInfoMapper;

    public StatsOverviewVO overview() {
        StatsOverviewVO vo = statsMapper.overview();
        if (vo == null) {
            vo = new StatsOverviewVO();
        }
        if (vo.getTodayOrderCount() == null) {
            vo.setTodayOrderCount(0L);
        }
        if (vo.getTodayRevenue() == null) {
            vo.setTodayRevenue(BigDecimal.ZERO);
        }
        if (vo.getTotalUsers() == null) {
            vo.setTotalUsers(0L);
        }
        if (vo.getAvgScore() == null) {
            vo.setAvgScore(BigDecimal.ZERO);
        }
        if (vo.getPendingOrders() == null) {
            vo.setPendingOrders(0L);
        }
        return vo;
    }

    public List<SalesTrendVO> salesTrend(Integer days) {
        int span = days == null || days <= 0 ? 7 : Math.min(days, 30);
        LocalDate start = LocalDate.now().minusDays(span - 1L);
        return statsMapper.salesTrend(start.atStartOfDay());
    }

    public List<TopDishVO> topDishes(Integer days, Integer limit) {
        int span = days == null || days <= 0 ? 7 : Math.min(days, 30);
        int size = limit == null || limit <= 0 ? 10 : Math.min(limit, 20);
        return statsMapper.topDishes(LocalDate.now().minusDays(span).atStartOfDay(), size);
    }

    /**
     * 导出订单 CSV（带 UTF-8 BOM，Excel 直接打开不乱码）。
     */
    public byte[] exportOrders(LocalDate startDate, LocalDate endDate) {
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        LocalDate start = startDate == null ? end.minusDays(6) : startDate;
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = end.plusDays(1).atStartOfDay();
        List<OrderInfo> orders = orderInfoMapper.selectList(Wrappers.<OrderInfo>lambdaQuery()
                .in(OrderInfo::getStatus, PAID_STATUS)
                .ge(OrderInfo::getCreateTime, from)
                .lt(OrderInfo::getCreateTime, to)
                .orderByAsc(OrderInfo::getCreateTime));

        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("订单号,下单时间,支付时间,状态,支付金额,用户ID,档口ID\n");
        for (OrderInfo order : orders) {
            csv.append(escape(order.getOrderNo())).append(',')
                    .append(order.getCreateTime() == null ? "" : TIME.format(order.getCreateTime())).append(',')
                    .append(order.getPayTime() == null ? "" : TIME.format(order.getPayTime())).append(',')
                    .append(OrderStatus.of(order.getStatus()).getText()).append(',')
                    .append(order.getPayAmount() == null ? "0.00" : order.getPayAmount().toPlainString()).append(',')
                    .append(order.getUserId()).append(',')
                    .append(order.getStallId()).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
