package com.scaffold.framework.config;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.scaffold.common.config.ScaffoldConfig;
import com.scaffold.common.constant.Constants;
import com.scaffold.framework.interceptor.RepeatSubmitInterceptor;

/**
 * 通用配置。
 * <p>
 * CORS 默认仍然兼容历史的 {@code allowedOriginPattern=*}，可以通过 {@code cors.allowed-origins}
 * 显式收敛白名单（多个用逗号分隔）。生产环境建议显式配置而不是放任通配。
 *
 * @author scaffold
 */
@Configuration
public class ResourcesConfig implements WebMvcConfigurer
{
    @Autowired
    private RepeatSubmitInterceptor repeatSubmitInterceptor;

    @Value("${cors.allowed-origins:}")
    private String allowedOrigins;

    @Value("${cors.allowed-headers:*}")
    private String allowedHeaders;

    @Value("${cors.allowed-methods:*}")
    private String allowedMethods;

    @Value("${cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${cors.max-age:1800}")
    private long maxAge;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry)
    {
        registry.addResourceHandler(Constants.RESOURCE_PREFIX + "/**")
                .addResourceLocations("file:" + ScaffoldConfig.getProfile() + "/");

        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/springfox-swagger-ui/")
                .setCacheControl(CacheControl.maxAge(5, TimeUnit.HOURS).cachePublic());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        registry.addInterceptor(repeatSubmitInterceptor).addPathPatterns("/**");
    }

    @Bean
    public CorsFilter corsFilter()
    {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = parseList(allowedOrigins);
        if (origins.isEmpty())
        {
            config.addAllowedOriginPattern("*");
        }
        else
        {
            origins.forEach(config::addAllowedOriginPattern);
        }
        parseList(allowedHeaders).forEach(config::addAllowedHeader);
        parseList(allowedMethods).forEach(config::addAllowedMethod);
        if (config.getAllowedHeaders() == null || config.getAllowedHeaders().isEmpty())
        {
            config.addAllowedHeader("*");
        }
        if (config.getAllowedMethods() == null || config.getAllowedMethods().isEmpty())
        {
            config.addAllowedMethod("*");
        }
        config.setAllowCredentials(allowCredentials);
        config.setMaxAge(maxAge);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    private List<String> parseList(String raw)
    {
        if (raw == null || raw.isBlank())
        {
            return java.util.Collections.emptyList();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}