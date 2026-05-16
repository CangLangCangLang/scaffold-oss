package com.scaffold.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口幂等注解。
 * <p>
 * 与 {@link RepeatSubmit} 的差异：
 * <ul>
 *   <li>{@code @RepeatSubmit} 基于 Session/请求体哈希，仅防同一 tab 内的重复点击；</li>
 *   <li>{@code @Idempotent} 基于 Redis 分布式锁（SET NX EX），跨实例、跨会话生效，
 *   key 可结合 SpEL 引用方法参数，例如 {@code #userId + ':' + #req.bizNo}。</li>
 * </ul>
 *
 * @author scaffold
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent
{
    /**
     * 业务前缀，最终 Redis key = "idempotent:" + prefix + ":" + 解析后的 key。
     */
    String prefix() default "default";

    /**
     * 幂等键，支持 SpEL 表达式（可访问方法参数、{@code #user}、{@code #ip} 等）。
     * 若为空则取 「方法签名 + 用户名 + 请求体哈希」作为兜底。
     */
    String key() default "";

    /**
     * 幂等窗口，单位秒，默认 5 秒。
     */
    int expire() default 5;

    /**
     * 命中重复请求时返回的提示语。
     */
    String message() default "操作正在处理中，请勿重复提交";
}
