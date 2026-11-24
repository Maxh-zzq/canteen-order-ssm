package com.xatu.canteen.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xatu.canteen.common.BizException;
import com.xatu.canteen.common.CanteenProperties;
import com.xatu.canteen.common.IdGenerator;
import com.xatu.canteen.common.OrderStatus;
import com.xatu.canteen.common.PageResult;
import com.xatu.canteen.common.RedisKeys;
import com.xatu.canteen.common.ResultCode;
import com.xatu.canteen.dto.PlaceOrderDTO;
import com.xatu.canteen.entity.CartItem;
import com.xatu.canteen.entity.DailyMenu;
import com.xatu.canteen.entity.Dish;
import com.xatu.canteen.entity.DishSpec;
import com.xatu.canteen.entity.OrderInfo;
import com.xatu.canteen.entity.OrderItem;
import com.xatu.canteen.entity.OrderStatusLog;
import com.xatu.canteen.entity.Stall;
import com.xatu.canteen.mapper.DailyMenuMapper;
import com.xatu.canteen.mapper.OrderInfoMapper;
import com.xatu.canteen.mapper.OrderItemMapper;
import com.xatu.canteen.mapper.OrderStatusLogMapper;
import com.xatu.canteen.mapper.StallMapper;
import com.xatu.canteen.security.LoginUser;
import com.xatu.canteen.security.SecurityUtils;
import com.xatu.canteen.vo.OrderItemVO;
import com.xatu.canteen.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 订单服务：下单（幂等 + 库存预扣 + 单档口约束）、状态机流转、取消、超时处理、取餐核销。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Duration IDEMPOTENT_TTL = Duration.ofMinutes(30);

    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;
    private final CartService cartService;
    private final DishService dishService;
    private final MenuService menuService;
    private final StockService stockService;
    private final DelayQueueService delayQueueService;
    private final DailyMenuMapper dailyMenuMapper;
    private final StallMapper stallMapper;
    private final StaffStallService staffStallService;
    private final IdGenerator idGenerator;
    private final CanteenProperties properties;
    private final StringRedisTemplate redis;

    @Transactional(rollbackFor = Exception.class)
    public OrderVO placeOrder(PlaceOrderDTO dto) {
        LoginUser user = SecurityUtils.currentUser();
        Long userId = user.getId();
        String idemKey = RedisKeys.orderIdempotent(userId, dto.getRequestId());
        if (!tryAcquireIdempotent(idemKey)) {
            OrderInfo exists = orderInfoMapper.selectOne(Wrappers.<OrderInfo>lambdaQuery()
                    .eq(OrderInfo::getUserId, userId)
                    .eq(OrderInfo::getRequestId, dto.getRequestId()));
            if (exists != null) {
                return detail(exists.getOrderNo());
            }
            throw new BizException(ResultCode.DUPLICATE_REQUEST);
        }

        List<StockDeduct> deducted = new ArrayList<>();
        try {
            LocalDateTime now = LocalDateTime.now();
            if (dto.getExpectTime() != null && dto.getExpectTime().isBefore(now)) {
                throw new BizException(ResultCode.BAD_REQUEST, "期望取餐时间不能早于当前时间");
            }
            List<CartItem> lines = cartService.loadRawItems(userId);
            if (CollectionUtils.isEmpty(lines)) {
                throw new BizException(ResultCode.CART_EMPTY);
            }
            List<Long> dishIds = lines.stream().map(CartItem::getDishId).distinct().collect(Collectors.toList());
            Map<Long, DailyMenu> menus = menuService.findTodayMenus(dishIds);

            Long stallId = null;
            BigDecimal total = BigDecimal.ZERO;
            List<OrderItem> items = new ArrayList<>();
            for (CartItem line : lines) {
                int quantity = line.getQuantity() == null ? 0 : line.getQuantity();
                if (quantity <= 0) {
                    continue;
                }
                Dish dish = dishService.requireDish(line.getDishId());
                DishSpec spec = dishService.requireSpec(dish.getId(), line.getSpecId());
                if (stallId == null) {
                    stallId = dish.getStallId();
                } else if (!Objects.equals(stallId, dish.getStallId())) {
                    throw new BizException(ResultCode.CROSS_STALL_ORDER);
                }
                DailyMenu menu = menus.get(dish.getId());
                if (menu == null) {
                    throw new BizException(ResultCode.MENU_NOT_FOUND, "「" + dish.getName() + "」今日未供应");
                }
                if (!stockService.tryDeduct(menu.getId(), quantity)) {
                    throw new BizException(ResultCode.STOCK_NOT_ENOUGH, "「" + dish.getName() + "」库存不足");
                }
                int rows = dailyMenuMapper.deductStock(menu.getId(), quantity);
                if (rows == 0) {
                    stockService.restoreCache(menu.getId(), quantity);
                    throw new BizException(ResultCode.STOCK_NOT_ENOUGH, "「" + dish.getName() + "」库存不足");
                }
                deducted.add(new StockDeduct(menu.getId(), quantity));

                BigDecimal unitPrice = dish.getPrice();
                if (spec != null && spec.getPriceDiff() != null) {
                    unitPrice = unitPrice.add(spec.getPriceDiff());
                }
                unitPrice = unitPrice.setScale(2, RoundingMode.HALF_UP);
                BigDecimal amount = unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);

                OrderItem item = new OrderItem();
                item.setDishId(dish.getId());
                item.setDishName(dish.getName());
                item.setSpecName(spec == null ? null : spec.getName());
                item.setPrice(unitPrice);
                item.setQuantity(quantity);
                item.setAmount(amount);
                item.setMenuId(menu.getId());
                items.add(item);
                total = total.add(amount);
            }
            if (items.isEmpty() || stallId == null) {
                throw new BizException(ResultCode.CART_EMPTY);
            }

            OrderInfo order = new OrderInfo();
            order.setOrderNo(idGenerator.nextOrderNo());
            order.setUserId(userId);
            order.setStallId(stallId);
            order.setStatus(OrderStatus.PENDING_PAY.getCode());
            order.setTotalAmount(total);
            order.setPayAmount(total);
            order.setPickupCode(idGenerator.nextPickupCode());
            order.setExpectTime(dto.getExpectTime());
            order.setExpireTime(now.plusMinutes(properties.getOrder().getTimeoutMinutes()));
            order.setRemark(dto.getRemark());
            order.setRequestId(dto.getRequestId());
            order.setVersion(0);
            orderInfoMapper.insert(order);

            for (OrderItem item : items) {
                item.setOrderId(order.getId());
                item.setOrderNo(order.getOrderNo());
                orderItemMapper.insert(item);
            }
            writeStatusLog(order, null, OrderStatus.PENDING_PAY.getCode(),
                    userId, user.getUsername(), "创建订单");

            cartService.clear(userId);
            scheduleDelay(order.getOrderNo(), order.getExpireTime());
            return buildVO(order, items);
        } catch (Exception e) {
            // 数据库扣减随事务回滚，Redis 预扣必须手动补偿
            for (StockDeduct deduct : deducted) {
                stockService.restoreCache(deduct.menuId, deduct.quantity);
            }
            safeDelete(idemKey);
            throw e;
        }
    }

    public OrderVO detail(String orderNo) {
        LoginUser user = SecurityUtils.currentUser();
        OrderInfo order = requireOrder(orderNo);
        if ("ADMIN".equals(user.getRole())) {
            // 系统管理员可查看全部订单，用于后台排查与审计
        } else if ("STAFF".equals(user.getRole())) {
            staffStallService.assertStallAccess(order.getStallId());
        } else if (!Objects.equals(order.getUserId(), user.getId())) {
            throw new BizException(ResultCode.ORDER_OWNER_ERROR);
        }
        List<OrderItem> items = listItems(order.getId());
        return buildVO(order, items);
    }

    public PageResult<OrderVO> listMine(Integer status, Integer pageNum, Integer pageSize) {
        Long userId = SecurityUtils.currentUserId();
        Page<OrderInfo> page = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize);
        orderInfoMapper.selectPage(page, Wrappers.<OrderInfo>lambdaQuery()
                .eq(OrderInfo::getUserId, userId)
                .eq(status != null, OrderInfo::getStatus, status)
                .orderByDesc(OrderInfo::getCreateTime));
        List<OrderVO> vos = page.getRecords().stream().map(this::buildVO).collect(Collectors.toList());
        return PageResult.of(page.getTotal(), page.getPages(), page.getCurrent(), page.getSize(), vos);
    }

    public PageResult<OrderVO> staffList(Integer status, Integer pageNum, Integer pageSize) {
        LoginUser user = SecurityUtils.currentUser();
        Set<Long> allowedStalls = null;
        if (!"ADMIN".equals(user.getRole())) {
            allowedStalls = staffStallService.activeStallIds(user.getId());
            if (allowedStalls.isEmpty()) {
                return PageResult.of(0, 0,
                        pageNum == null ? 1 : pageNum,
                        pageSize == null ? 10 : pageSize,
                        Collections.emptyList());
            }
        }

        Page<OrderInfo> page = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize);
        List<Integer> defaults = new ArrayList<>();
        defaults.add(OrderStatus.PAID.getCode());
        defaults.add(OrderStatus.PREPARING.getCode());
        defaults.add(OrderStatus.READY.getCode());
        orderInfoMapper.selectPage(page, Wrappers.<OrderInfo>lambdaQuery()
                .in(allowedStalls != null, OrderInfo::getStallId, allowedStalls)
                .in(status == null, OrderInfo::getStatus, defaults)
                .eq(status != null, OrderInfo::getStatus, status)
                .orderByAsc(OrderInfo::getCreateTime));
        List<OrderVO> vos = new ArrayList<>();
        for (OrderInfo order : page.getRecords()) {
            vos.add(buildVO(order, listItems(order.getId())));
        }
        return PageResult.of(page.getTotal(), page.getPages(), page.getCurrent(), page.getSize(), vos);
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(String orderNo, String reason) {
        LoginUser user = SecurityUtils.currentUser();
        OrderInfo order = requireOrder(orderNo);
        if (!"ADMIN".equals(user.getRole()) && !Objects.equals(order.getUserId(), user.getId())) {
            throw new BizException(ResultCode.ORDER_OWNER_ERROR);
        }
        if (order.getStatus() == null || order.getStatus() != OrderStatus.PENDING_PAY.getCode()) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR, "仅待支付订单可以取消");
        }
        restoreStock(order);
        order.setCancelReason(reason == null || reason.trim().isEmpty() ? "用户取消" : reason);
        transit(order, OrderStatus.CANCELLED, user.getId(), user.getUsername(), order.getCancelReason());
        delayQueueService.remove(orderNo);
    }

    @Transactional(rollbackFor = Exception.class)
    public void accept(String orderNo) {
        LoginUser user = SecurityUtils.currentUser();
        OrderInfo order = requireOrder(orderNo);
        staffStallService.assertStallAccess(order.getStallId());
        transit(order, OrderStatus.PREPARING, user.getId(), user.getUsername(), "档口接单");
    }

    @Transactional(rollbackFor = Exception.class)
    public void ready(String orderNo) {
        LoginUser user = SecurityUtils.currentUser();
        OrderInfo order = requireOrder(orderNo);
        staffStallService.assertStallAccess(order.getStallId());
        transit(order, OrderStatus.READY, user.getId(), user.getUsername(), "出餐完成");
    }

    @Transactional(rollbackFor = Exception.class)
    public void pickup(String orderNo, String pickupCode) {
        LoginUser user = SecurityUtils.currentUser();
        OrderInfo order = requireOrder(orderNo);
        staffStallService.assertStallAccess(order.getStallId());
        if (order.getStatus() == null || order.getStatus() != OrderStatus.READY.getCode()) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR, "订单未进入可取餐状态");
        }
        if (!Objects.equals(order.getPickupCode(), pickupCode)) {
            throw new BizException(ResultCode.PICKUP_CODE_ERROR);
        }
        transit(order, OrderStatus.COMPLETED, user.getId(), user.getUsername(), "取餐核销");
    }

    /**
     * 超时未支付自动取消。由延迟队列消费者调用，必须幂等：状态已变则直接返回。
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleTimeout(String orderNo) {
        OrderInfo order = orderInfoMapper.selectOne(Wrappers.<OrderInfo>lambdaQuery()
                .eq(OrderInfo::getOrderNo, orderNo));
        if (order == null || order.getStatus() == null
                || order.getStatus() != OrderStatus.PENDING_PAY.getCode()) {
            return;
        }
        if (order.getExpireTime() != null && order.getExpireTime().isAfter(LocalDateTime.now())) {
            return;
        }
        restoreStock(order);
        order.setCancelReason("超时未支付，系统自动取消");
        transit(order, OrderStatus.CANCELLED, 0L, "SYSTEM", order.getCancelReason());
        log.info("订单超时自动取消: {}", orderNo);
    }

    /**
     * 统一状态流转入口。校验合法性、刷新时间字段、写流转日志。
     */
    @Transactional(rollbackFor = Exception.class)
    public void transit(OrderInfo order, OrderStatus target, Long operatorId, String operatorName, String remark) {
        OrderStatus from = OrderStatus.of(order.getStatus());
        if (!from.canTransitionTo(target)) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR,
                    "订单当前为「" + from.getText() + "」，不能变更为「" + target.getText() + "」");
        }
        order.setStatus(target.getCode());
        if (target == OrderStatus.PAID) {
            order.setPayTime(LocalDateTime.now());
        }
        if (target == OrderStatus.COMPLETED) {
            order.setFinishTime(LocalDateTime.now());
        }
        int rows = orderInfoMapper.updateById(order);
        if (rows == 0) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR, "订单状态已变更，请刷新后重试");
        }
        writeStatusLog(order, from.getCode(), target.getCode(), operatorId, operatorName, remark);
    }

    public OrderInfo requireOrder(String orderNo) {
        OrderInfo order = orderInfoMapper.selectOne(Wrappers.<OrderInfo>lambdaQuery()
                .eq(OrderInfo::getOrderNo, orderNo));
        if (order == null) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    public List<OrderItem> listItems(Long orderId) {
        return orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, orderId));
    }

    public void restoreStock(OrderInfo order) {
        List<OrderItem> items = listItems(order.getId());
        for (OrderItem item : items) {
            if (item.getMenuId() != null && item.getQuantity() != null) {
                stockService.restore(item.getMenuId(), item.getQuantity());
            }
        }
    }

    private OrderVO buildVO(OrderInfo order) {
        OrderVO vo = OrderVO.from(order);
        Stall stall = order.getStallId() == null ? null : stallMapper.selectById(order.getStallId());
        if (stall != null) {
            vo.setStallName(stall.getName());
        }
        return vo;
    }

    private OrderVO buildVO(OrderInfo order, List<OrderItem> items) {
        OrderVO vo = buildVO(order);
        List<OrderItemVO> itemVOs = new ArrayList<>();
        for (OrderItem item : items) {
            OrderItemVO itemVO = new OrderItemVO();
            itemVO.setDishId(item.getDishId());
            itemVO.setDishName(item.getDishName());
            itemVO.setSpecName(item.getSpecName());
            itemVO.setPrice(item.getPrice());
            itemVO.setQuantity(item.getQuantity());
            itemVO.setAmount(item.getAmount());
            itemVOs.add(itemVO);
        }
        vo.setItems(itemVOs);
        return vo;
    }

    private void writeStatusLog(OrderInfo order, Integer fromStatus, Integer toStatus,
                                Long operatorId, String operatorName, String remark) {
        OrderStatusLog statusLog = new OrderStatusLog();
        statusLog.setOrderId(order.getId());
        statusLog.setOrderNo(order.getOrderNo());
        statusLog.setFromStatus(fromStatus);
        statusLog.setToStatus(toStatus);
        statusLog.setOperatorId(operatorId);
        statusLog.setOperatorName(operatorName);
        statusLog.setRemark(remark);
        orderStatusLogMapper.insert(statusLog);
    }

    private boolean tryAcquireIdempotent(String key) {
        try {
            Boolean success = redis.opsForValue().setIfAbsent(key, "1", IDEMPOTENT_TTL);
            return Boolean.TRUE.equals(success);
        } catch (Exception e) {
            log.warn("Redis 幂等校验失败，依赖数据库唯一索引: {}", e.getMessage());
            return true;
        }
    }

    private void safeDelete(String key) {
        try {
            redis.delete(key);
        } catch (Exception e) {
            log.warn("删除幂等键失败: {}", e.getMessage());
        }
    }

    private void scheduleDelay(String orderNo, LocalDateTime expireAt) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    delayQueueService.add(orderNo, expireAt);
                }
            });
        } else {
            delayQueueService.add(orderNo, expireAt);
        }
    }

    private static class StockDeduct {
        private final Long menuId;
        private final int quantity;

        StockDeduct(Long menuId, int quantity) {
            this.menuId = menuId;
            this.quantity = quantity;
        }
    }
}
