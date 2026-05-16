package com.scaffold.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解，基于 Redis 计数实现的固定窗口算法。
 * <p>
 * 维度：
 * <ul>
 *   <li>{@link LimitType#DEFAULT} 全局公共池</li>
 *   <li>{@link LimitType#IP} 按调用方 IP 维度</li>
 *   <li>{@link LimitType#USER} 按登录用户名维度（未登录则退化为 IP）</li>
 * </ul>
 *
 * @author scaffold
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit
{
    /** 维度类型 */
    enum LimitType { DEFAULT, IP, USER }

    /**
     * key 前缀。
     */
    String key() default "rate";

    /**
     * 时间窗口长度（秒）。
     */
    int period() default 60;

    /**
     * 时间窗口内允许的最大次数。
     */
    int count() default 100;

    /**
     * 维度。
     */
    LimitType limitType() default LimitType.DEFAULT;

    /**
     * 命中限流后的提示语。
     */
    String message() default "访问过于频繁，请稍后再试";
}
