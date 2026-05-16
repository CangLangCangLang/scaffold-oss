package com.scaffold.framework.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 提供 Jackson 2 的 {@link ObjectMapper} bean。
 * <p>
 * Spring Boot 4 起默认改用 Jackson 3（{@code tools.jackson}），不再自动配出 Jackson 2 的
 * {@code com.fasterxml.jackson.databind.ObjectMapper}。但本项目大量组件
 * （审计 diff、SSO 用户解析、Redis 序列化、inbox 等）仍依赖 Jackson 2，缺这个 bean 启动期会
 * 直接 {@code UnsatisfiedDependencyException}。
 * <p>
 * 暴露一个无状态的 ObjectMapper，{@code @ConditionalOnMissingBean} 兜底允许业务侧在需要时
 * 自己定制（例如注册全局模块）。
 *
 * @author scaffold
 */
@Configuration
public class Jackson2ObjectMapperConfig
{
    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper jackson2ObjectMapper()
    {
        return new ObjectMapper();
    }
}
