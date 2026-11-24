package com.xatu.canteen.common;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 雪花 ID 生成器，workerId 由配置指定（多实例部署时每个实例必须不同）。
 */
@Component
public class IdGenerator {

    private static final long EPOCH = 1700000000000L;
    private static final long MAX_WORKER_ID = 1023L;

    private final CanteenProperties properties;

    private long workerId;
    private long sequence;
    private long lastTimestamp = -1L;

    public IdGenerator(CanteenProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        long id = properties.getWorkerId() == null ? 1L : properties.getWorkerId();
        if (id < 0 || id > MAX_WORKER_ID) {
            throw new IllegalArgumentException("canteen.worker-id 必须在 0~1023 之间");
        }
        this.workerId = id;
    }

    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) {
            timestamp = lastTimestamp;
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & 4095L;
            if (sequence == 0) {
                while ((timestamp = System.currentTimeMillis()) <= lastTimestamp) {
                    // 等待下一毫秒
                }
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << 22) | (workerId << 12) | sequence;
    }

    /**
     * 订单号：CO + 雪花 ID。
     */
    public String nextOrderNo() {
        return "CO" + nextId();
    }

    /**
     * 6 位取餐码。
     */
    public String nextPickupCode() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }
}
