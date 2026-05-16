package com.scaffold.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API 版本注解，可以标注在 Controller 类或方法上。
 * <p>
 * 与 {@link com.scaffold.framework.web.version.ApiVersionRequestMappingHandlerMapping}
 * 协同工作；支持的写法（默认）：
 * <pre>
 * @ApiVersion(2)
 * @RequestMapping("/v{version}/order")
 * public class OrderController { ... }
 * </pre>
 * 客户端用 {@code /v2/order/...} 访问；如果客户端请求 {@code /v1/order/...}，
 * 在没有显式声明 v1 的方法时返回 404。
 *
 * @author scaffold
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiVersion
{
    /**
     * 该 Controller / 方法支持的最小版本号。版本与请求路径中的 {@code v{version}} 占位匹配。
     * 客户端请求版本 ≥ 该值即可命中（向上兼容）。
     */
    int value() default 1;
}
