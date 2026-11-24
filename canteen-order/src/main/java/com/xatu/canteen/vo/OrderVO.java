package com.xatu.canteen.vo;

import com.xatu.canteen.common.OrderStatus;
import com.xatu.canteen.entity.OrderInfo;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单详情。
 */
@Data
public class OrderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderNo;
    private Integer status;
    private String statusText;
    private BigDecimal totalAmount;
    private BigDecimal payAmount;
    private String pickupCode;
    private LocalDateTime expectTime;
    private LocalDateTime expireTime;
    private LocalDateTime payTime;
    private LocalDateTime finishTime;
    private LocalDateTime createTime;
    private Long stallId;
    private String stallName;
    private String remark;
    private String cancelReason;
    private Boolean canCancel;
    private List<OrderItemVO> items;

    public static OrderVO from(OrderInfo order) {
        if (order == null) {
            return null;
        }
        OrderVO vo = new OrderVO();
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setStatusText(OrderStatus.of(order.getStatus()).getText());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setPickupCode(order.getPickupCode());
        vo.setExpectTime(order.getExpectTime());
        vo.setExpireTime(order.getExpireTime());
        vo.setPayTime(order.getPayTime());
        vo.setFinishTime(order.getFinishTime());
        vo.setCreateTime(order.getCreateTime());
        vo.setStallId(order.getStallId());
        vo.setRemark(order.getRemark());
        vo.setCancelReason(order.getCancelReason());
        vo.setCanCancel(order.getStatus() != null && order.getStatus() == OrderStatus.PENDING_PAY.getCode());
        return vo;
    }
}
