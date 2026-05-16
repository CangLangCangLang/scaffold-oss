package com.scaffold.framework.observability;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Q-3 可观测性总配置：开启 {@link ObservabilityProperties} 绑定 + Scheduling + Async。
 *
 * <p>不挂 {@code @ConditionalOnProperty}：本配置类只引入开关 properties，
 * 真正的开关在 properties 自身的 {@code enabled} 字段（filter / job / binder 各自检查）。
 *
 * <p>{@code @EnableAsync} 是给 {@link HttpRequestRecorder#asyncInsert} 用的；
 * {@code @EnableScheduling} 是给 {@link BusinessMetricsBinder#refresh} 用的。
 */
@Configuration
@EnableConfigurationProperties(ObservabilityProperties.class)
@EnableAsync
@EnableScheduling
public class ObservabilityConfig
{
}
