package com.scaffold.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.scaffold.common.core.dict.DictTransSerializer;

/**
 * 字典翻译注解。
 * <p>
 * 标注在 DTO 字段上：序列化时会自动把字典 value 翻译成 label。
 * 使用 {@link JacksonAnnotationsInside} 把 {@link JsonSerialize} 包装在内，
 * 业务字段只需要写一个注解。
 * <p>
 * 示例：
 * <pre>
 * public class UserDto {
 *     {@literal @}DictTrans(type = "sys_user_sex")
 *     private String sex;            // 序列化结果：sex="0"，sexLabel="男"
 *
 *     {@literal @}DictTrans(type = "sys_normal_disable", target = "statusName")
 *     private String status;
 * }
 * </pre>
 *
 * @author scaffold
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = DictTransSerializer.class)
public @interface DictTrans
{
    /**
     * 字典类型，例如 {@code sys_user_sex}。
     */
    String type();

    /**
     * 标签字段名；默认在原字段名后追加 {@code Label}。
     */
    String target() default "";

    /**
     * 多值时的分隔符。
     */
    String separator() default ",";
}
