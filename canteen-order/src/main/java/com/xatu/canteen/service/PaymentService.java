package com.xatu.canteen.service;

import com.xatu.canteen.common.BizException;
import com.xatu.canteen.common.CanteenProperties;
import com.xatu.canteen.common.OrderStatus;
import com.xatu.canteen.common.ResultCode;
import com.xatu.canteen.dto.PayCallbackDTO;
import com.xatu.canteen.entity.OrderInfo;
import com.xatu.canteen.entity.OrderItem;
import com.xatu.canteen.entity.PaymentRecord;
import com.xatu.canteen.entity.RefundRecord;
import com.xatu.canteen.mapper.DishMapper;
import com.xatu.canteen.mapper.PaymentRecordMapper;
import com.xatu.canteen.mapper.RefundRecordMapper;
import com.xatu.canteen.mapper.SysUserMapper;
import com.xatu.canteen.security.LoginUser;
import com.xatu.canteen.security.SecurityUtils;
import com.xatu.canteen.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 支付服务：余额支付、模拟网关回调（验签 + 幂等）、退款。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderService orderService;
    private final SysUserMapper userMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final RefundRecordMapper refundRecordMapper;
    private final DishMapper dishMapper;
    private final DelayQueueService delayQueueService;
    private final CanteenProperties properties;

    /**
     * 余额支付：条件扣款防止并发扣成负数，再推进订单状态。
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderVO payByBalance(String orderNo) {
        LoginUser user = SecurityUtils.currentUser();
        OrderInfo order = orderService.requireOrder(orderNo);
        if (!Objects.equals(order.getUserId(), user.getId())) {
            throw new BizException(ResultCode.ORDER_OWNER_ERROR);
        }
        requirePending(order);
        int deducted = userMapper.deductBalance(user.getId(), order.getPayAmount());
        if (deducted == 0) {
            throw new BizException(ResultCode.BALANCE_NOT_ENOUGH);
        }
        LocalDateTime now = LocalDateTime.now();
        insertPayment(order, "BAL" + order.getOrderNo(), "BALANCE", order.getPayAmount(), now);
        order.setPayTime(now);
        orderService.transit(order, OrderStatus.PAID, user.getId(), user.getUsername(), "余额支付");
        increaseDishSales(order);
        delayQueueService.remove(orderNo);
        return orderService.detail(orderNo);
    }

    /**
     * 模拟网关异步回调。sign = MD5(orderNo + tradeNo + amount + secret)。
     * 重复回调直接返回 success，不重复入账。
     */
    @Transactional(rollbackFor = Exception.class)
    public String mockCallback(PayCallbackDTO dto) {
        verifySign(dto);
        OrderInfo order = orderService.requireOrder(dto.getOrderNo());
        if (dto.getAmount().compareTo(order.getPayAmount()) != 0) {
            throw new BizException(ResultCode.PAY_AMOUNT_ERROR);
        }
        if (order.getStatus() == null || order.getStatus() != OrderStatus.PENDING_PAY.getCode()) {
            if (order.getStatus() != null && OrderStatus.of(order.getStatus()).isPaid()) {
                return "success";
            }
            throw new BizException(ResultCode.ORDER_STATUS_ERROR, "订单已取消，无法支付");
        }
        if (order.getExpireTime() != null && order.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BizException(ResultCode.ORDER_EXPIRED);
        }
        LocalDateTime now = LocalDateTime.now();
        try {
            insertPayment(order, dto.getTradeNo(), "MOCK", dto.getAmount(), now);
        } catch (DuplicateKeyException e) {
            log.info("支付回调重复，直接返回成功 tradeNo={}", dto.getTradeNo());
            return "success";
        }
        order.setPayTime(now);
        orderService.transit(order, OrderStatus.PAID, 0L, "MOCK_GATEWAY", "模拟支付回调");
        increaseDishSales(order);
        delayQueueService.remove(order.getOrderNo());
        return "success";
    }

    /**
     * 退款：仅已支付/备餐中的订单，退回余额并回滚库存与销量。
     */
    @Transactional(rollbackFor = Exception.class)
    public void refund(String orderNo, String reason) {
        LoginUser operator = SecurityUtils.currentUser();
        OrderInfo order = orderService.requireOrder(orderNo);
        OrderStatus status = OrderStatus.of(order.getStatus());
        if (status != OrderStatus.PAID && status != OrderStatus.PREPARING) {
            throw new BizException(ResultCode.REFUND_NOT_ALLOWED, "当前状态「" + status.getText() + "」不支持退款");
        }
        userMapper.addBalance(order.getUserId(), order.getPayAmount());

        RefundRecord record = new RefundRecord();
        record.setOrderNo(orderNo);
        record.setRefundNo("RF" + orderNo);
        record.setAmount(order.getPayAmount());
        record.setStatus(1);
        record.setReason(StringUtils.hasText(reason) ? reason : "管理员退款");
        refundRecordMapper.insert(record);

        orderService.restoreStock(order);
        decreaseDishSales(order);
        orderService.transit(order, OrderStatus.REFUNDED, operator.getId(), operator.getUsername(),
                record.getReason());
    }

    private void requirePending(OrderInfo order) {
        if (order.getStatus() == null || order.getStatus() != OrderStatus.PENDING_PAY.getCode()) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR, "当前订单不可支付");
        }
        if (order.getExpireTime() != null && order.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BizException(ResultCode.ORDER_EXPIRED);
        }
    }

    private void insertPayment(OrderInfo order, String tradeNo, String channel, BigDecimal amount,
                               LocalDateTime callbackTime) {
        PaymentRecord record = new PaymentRecord();
        record.setOrderNo(order.getOrderNo());
        record.setTradeNo(tradeNo);
        record.setChannel(channel);
        record.setAmount(amount);
        record.setStatus(1);
        record.setCallbackTime(callbackTime);
        paymentRecordMapper.insert(record);
    }

    private void increaseDishSales(OrderInfo order) {
        List<OrderItem> items = orderService.listItems(order.getId());
        for (OrderItem item : items) {
            dishMapper.increaseSales(item.getDishId(), item.getQuantity());
        }
    }

    private void decreaseDishSales(OrderInfo order) {
        List<OrderItem> items = orderService.listItems(order.getId());
        for (OrderItem item : items) {
            dishMapper.decreaseSales(item.getDishId(), item.getQuantity());
        }
    }

    private void verifySign(PayCallbackDTO dto) {
        String amount = dto.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString();
        String raw = dto.getOrderNo() + dto.getTradeNo() + amount + properties.getPay().getSecret();
        String expected = DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
        if (!expected.equalsIgnoreCase(dto.getSign())) {
            throw new BizException(ResultCode.PAY_SIGN_ERROR);
        }
    }
}
