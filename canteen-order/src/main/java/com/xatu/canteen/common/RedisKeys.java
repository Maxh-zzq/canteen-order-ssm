package com.xatu.canteen.common;

/**
 * Redis Key 统一定义，全部带 canteen: 前缀，避免与同库其他应用冲突。
 */
public final class RedisKeys {

    private static final String PREFIX = "canteen:";

    private RedisKeys() {
    }

    public static String refreshToken(Long userId) {
        return PREFIX + "auth:refresh:" + userId;
    }

    public static String blacklist(String jti) {
        return PREFIX + "auth:black:" + jti;
    }

    public static String activeSession(Long userId) {
        return PREFIX + "auth:active:" + userId;
    }

    public static String loginFail(String username) {
        return PREFIX + "login:fail:" + username;
    }

    public static String captcha(String key) {
        return PREFIX + "captcha:" + key;
    }

    public static String cart(Long userId) {
        return PREFIX + "cart:" + userId;
    }

    public static String dishDetail(Long dishId) {
        return PREFIX + "dish:detail:" + dishId;
    }

    public static String dishNull(Long dishId) {
        return PREFIX + "dish:null:" + dishId;
    }

    public static String categoryList() {
        return PREFIX + "dish:category";
    }

    public static String todayMenu(String date) {
        return PREFIX + "menu:today:" + date;
    }

    public static String menuStock(Long menuId) {
        return PREFIX + "stock:menu:" + menuId;
    }

    public static String orderIdempotent(Long userId, String requestId) {
        return PREFIX + "order:idem:" + userId + ":" + requestId;
    }

    public static String orderDelayQueue() {
        return PREFIX + "order:delay";
    }

    public static String orderLock(String orderNo) {
        return PREFIX + "lock:order:" + orderNo;
    }

    public static String rateLimit(String key, String identity) {
        return PREFIX + "rate:" + key + ":" + identity;
    }

    public static String cacheLock(String key) {
        return PREFIX + "lock:cache:" + key;
    }
}
