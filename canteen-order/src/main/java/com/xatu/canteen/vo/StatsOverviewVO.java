package com.xatu.canteen.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 经营概览。
 */
@Data
public class StatsOverviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long todayOrderCount;
    private BigDecimal todayRevenue;
    private Long totalUsers;
    private BigDecimal avgScore;
    private Long pendingOrders;
}
