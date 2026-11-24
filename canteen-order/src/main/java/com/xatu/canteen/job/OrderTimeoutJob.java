package com.xatu.canteen.job;

import com.xatu.canteen.service.DelayQueueService;
import com.xatu.canteen.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 订单超时消费者：每 10 秒扫一次延迟队列，取消超时未支付订单并回滚库存。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutJob {

    private static final int BATCH_SIZE = 20;

    private final DelayQueueService delayQueueService;
    private final OrderService orderService;

    @Scheduled(fixedDelay = 10000, initialDelay = 10000)
    public void poll() {
        Set<String> due = delayQueueService.pollDue(BATCH_SIZE);
        for (String orderNo : due) {
            try {
                orderService.handleTimeout(orderNo);
            } catch (Exception e) {
                log.error("处理超时订单失败 orderNo={}", orderNo, e);
            }
        }
    }
}
