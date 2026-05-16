package com.scaffold.framework.web.version;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.mvc.condition.RequestCondition;

/**
 * API 版本条件：合并时取较高版本，匹配时要求请求版本 ≥ 注解版本。
 *
 * @author scaffold
 */
public class ApiVersionCondition implements RequestCondition<ApiVersionCondition>
{
    private final int apiVersion;

    public ApiVersionCondition(int apiVersion)
    {
        this.apiVersion = apiVersion;
    }

    public int getApiVersion()
    {
        return apiVersion;
    }

    /**
     * 类与方法都标注时取较高的（方法优先）。
     */
    @Override
    public ApiVersionCondition combine(ApiVersionCondition other)
    {
        return new ApiVersionCondition(other.getApiVersion());
    }

    @Override
    public ApiVersionCondition getMatchingCondition(HttpServletRequest request)
    {
        int requestVersion = ApiVersionRequestMappingHandlerMapping.parseRequestVersion(request.getRequestURI());
        return requestVersion >= apiVersion ? this : null;
    }

    @Override
    public int compareTo(ApiVersionCondition other, HttpServletRequest request)
    {
        // 优先匹配高版本
        return Integer.compare(other.apiVersion, this.apiVersion);
    }
}
