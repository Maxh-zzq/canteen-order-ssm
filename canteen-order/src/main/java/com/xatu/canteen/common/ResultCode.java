package com.xatu.canteen.common;

import lombok.Getter;

/**
 * 业务码。10xxx 通用，20xxx 用户，30xxx 菜品，40xxx 购物车/订单，50xxx 支付，60xxx 评价。
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有操作权限"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "数据冲突"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),
    SERVER_ERROR(500, "系统繁忙，请稍后再试"),

    DUPLICATE_REQUEST(10001, "请勿重复提交"),
    ILLEGAL_STATE(10002, "状态异常，请刷新后重试"),

    USER_NOT_FOUND(20101, "用户不存在"),
    PASSWORD_ERROR(20102, "用户名或密码错误"),
    USER_DISABLED(20103, "账号已被禁用"),
    CAPTCHA_ERROR(20104, "验证码错误或已过期"),
    BALANCE_NOT_ENOUGH(20201, "余额不足"),
    STAFF_STALL_NOT_BOUND(20202, "当前员工未绑定档口，请联系管理员"),

    DISH_NOT_FOUND(30101, "菜品不存在"),
    DISH_OFF_SALE(30102, "菜品已下架"),
    STOCK_NOT_ENOUGH(30103, "库存不足"),
    MENU_NOT_FOUND(30104, "今日未供应该菜品"),
    CATEGORY_NOT_FOUND(30105, "分类不存在"),
    STALL_NOT_FOUND(30106, "档口不存在"),

    CART_EMPTY(40101, "购物车为空"),
    CART_ITEM_NOT_FOUND(40102, "购物车条目不存在"),
    CROSS_STALL_ORDER(40103, "同一订单只能包含一个档口的菜品"),

    ORDER_NOT_FOUND(40201, "订单不存在"),
    ORDER_STATUS_ERROR(40202, "当前订单状态不允许该操作"),
    ORDER_EXPIRED(40203, "订单已超时取消"),
    ORDER_OWNER_ERROR(40204, "无权操作该订单"),
    PICKUP_CODE_ERROR(40205, "取餐码不正确"),
    ORDER_STALL_FORBIDDEN(40206, "无权操作该档口订单"),

    PAY_AMOUNT_ERROR(50101, "支付金额与订单不符"),
    PAY_SIGN_ERROR(50102, "支付签名校验失败"),
    REFUND_NOT_ALLOWED(50103, "当前订单不支持退款"),

    REVIEW_EXISTS(60101, "该订单已评价"),
    REVIEW_NOT_ALLOWED(60102, "订单未完成，无法评价");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
