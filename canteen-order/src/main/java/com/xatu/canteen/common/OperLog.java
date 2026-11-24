package com.xatu.canteen.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 管理端操作日志。标注在需要审计的写接口上。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperLog {

    /** 模块名，例如「菜品管理」 */
    String module();

    /** 动作描述，默认用方法名 */
    String action() default "";
}
