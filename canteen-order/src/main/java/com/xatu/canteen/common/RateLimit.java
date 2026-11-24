package com.xatu.canteen.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流：基于 Redis 计数器，按用户或 IP 维度限制。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 业务键，默认用方法名 */
    String key() default "";

    /** 时间窗口内允许的次数 */
    int count() default 10;

    /** 时间窗口，秒 */
    int seconds() default 60;
}
