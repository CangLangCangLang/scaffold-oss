package com.scaffold.framework.web.version;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import com.scaffold.common.annotation.ApiVersion;

/**
 * 让 Spring MVC 识别 {@link ApiVersion} 注解，把 {@code /v{version}/...} 中的版本号映射
 * 为只允许 ≥ 注解声明版本的请求（向上兼容）。
 *
 * @author scaffold
 */
public class ApiVersionRequestMappingHandlerMapping extends RequestMappingHandlerMapping
{
    /** 路径中 v{version} 的提取正则，例如 /v2/order */
    private static final Pattern VERSION_PREFIX_PATTERN = Pattern.compile("/v(\\d+)/");

    @Override
    protected RequestCondition<ApiVersionCondition> getCustomTypeCondition(Class<?> handlerType)
    {
        ApiVersion apiVersion = AnnotationUtils.findAnnotation(handlerType, ApiVersion.class);
        return apiVersion == null ? null : new ApiVersionCondition(apiVersion.value());
    }

    @Override
    protected RequestCondition<ApiVersionCondition> getCustomMethodCondition(Method method)
    {
        ApiVersion apiVersion = AnnotationUtils.findAnnotation(method, ApiVersion.class);
        return apiVersion == null ? null : new ApiVersionCondition(apiVersion.value());
    }

    /**
     * 将请求 URI 中的 {@code /v{n}/} 转换为整型版本号。
     */
    static int parseRequestVersion(String requestUri)
    {
        if (requestUri == null) return 0;
        Matcher matcher = VERSION_PREFIX_PATTERN.matcher(requestUri);
        if (matcher.find())
        {
            try
            {
                return Integer.parseInt(matcher.group(1));
            }
            catch (NumberFormatException ignored)
            {
            }
        }
        return 0;
    }

    /**
     * 占位：在 {@link ApiVersionCondition} 内部使用。
     */
    @SuppressWarnings("unused")
    static RequestMappingInfo dummy()
    {
        return null;
    }
}
