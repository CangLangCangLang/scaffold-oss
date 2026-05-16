package com.scaffold.framework.config;

import org.springframework.boot.webmvc.autoconfigure.WebMvcRegistrations;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import com.scaffold.framework.web.version.ApiVersionRequestMappingHandlerMapping;

/**
 * 注入 {@link ApiVersionRequestMappingHandlerMapping}，让
 * {@code @ApiVersion} 与 {@code /v{version}/...} 路径自动联动。
 *
 * @author scaffold
 */
@Configuration
public class WebMvcCustomizeConfig implements WebMvcRegistrations
{
    @Override
    public RequestMappingHandlerMapping getRequestMappingHandlerMapping()
    {
        return new ApiVersionRequestMappingHandlerMapping();
    }
}
