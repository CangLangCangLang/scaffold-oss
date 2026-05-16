package com.scaffold.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.scaffold.common.core.json.SensitiveJsonSerializer;

/**
 * 字段脱敏注解，适用于 Jackson 序列化。
 * <p>
 * 在响应 DTO / 操作日志参数拼装时自动按指定策略脱敏，避免敏感数据落库或外泄。
 * 例如：{@code @SensitiveLog(strategy = SensitiveStrategy.MOBILE) private String phone;}
 *
 * @author scaffold
 */
@Target({ ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = SensitiveJsonSerializer.class)
public @interface SensitiveLog
{
    /**
     * 脱敏策略。
     */
    SensitiveStrategy strategy() default SensitiveStrategy.DEFAULT;

    /**
     * 自定义保留前缀长度（仅 {@link SensitiveStrategy#CUSTOM} 时生效）。
     */
    int prefixKeep() default 0;

    /**
     * 自定义保留后缀长度（仅 {@link SensitiveStrategy#CUSTOM} 时生效）。
     */
    int suffixKeep() default 0;

    /**
     * 替代字符。
     */
    String mask() default "*";
}
