package com.xatu.canteen.common;

import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 订单状态机。业务代码禁止直接 setStatus，必须走 {@code OrderService#transit}。
 */
@Getter
public enum OrderStatus {

    PENDING_PAY(10, "待支付"),
    PAID(20, "已支付"),
    PREPARING(30, "备餐中"),
    READY(40, "可取餐"),
    COMPLETED(50, "已完成"),
    CANCELLED(60, "已取消"),
    REFUNDED(70, "已退款");

    private final int code;
    private final String text;

    OrderStatus(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public static OrderStatus of(int code) {
        return Arrays.stream(values()).filter(s -> s.code == code).findFirst()
                .orElseThrow(() -> new BizException(ResultCode.ILLEGAL_STATE, "未知订单状态: " + code));
    }

    /**
     * 是否允许从当前状态流转到目标状态。
     */
    public boolean canTransitionTo(OrderStatus target) {
        return ALLOWED.getOrDefault(this, Collections.emptySet()).contains(target);
    }

    public boolean isPaid() {
        return this == PAID || this == PREPARING || this == READY || this == COMPLETED;
    }

    private static final Set<OrderStatus> PENDING_TARGETS =
            new HashSet<>(Arrays.asList(PAID, CANCELLED));
    private static final Set<OrderStatus> PAID_TARGETS =
            new HashSet<>(Arrays.asList(PREPARING, REFUNDED));
    private static final Set<OrderStatus> PREPARING_TARGETS =
            new HashSet<>(Arrays.asList(READY, REFUNDED));
    private static final Set<OrderStatus> READY_TARGETS =
            new HashSet<>(Collections.singletonList(COMPLETED));

    private static final java.util.Map<OrderStatus, Set<OrderStatus>> ALLOWED;

    static {
        java.util.Map<OrderStatus, Set<OrderStatus>> map = new java.util.HashMap<>();
        map.put(PENDING_PAY, PENDING_TARGETS);
        map.put(PAID, PAID_TARGETS);
        map.put(PREPARING, PREPARING_TARGETS);
        map.put(READY, READY_TARGETS);
        map.put(COMPLETED, Collections.emptySet());
        map.put(CANCELLED, Collections.emptySet());
        map.put(REFUNDED, Collections.emptySet());
        ALLOWED = Collections.unmodifiableMap(map);
    }
}
