package com.scaffold.framework.observability;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 可观测性开关 + 阈值（{@code app.observability.*}）。
 *
 * <ul>
 *   <li>{@code enabled}：总开关；false 则 HTTP 录入 / Health Indicator / 业务指标 / 告警 Job 全部不装</li>
 *   <li>{@code slowMs}：单接口耗时阈值，超过即落 sys_slow_request（默认 2000ms）</li>
 *   <li>{@code recordClientError}：4xx 是否记录（默认不记 — 业务校验失败太频繁）</li>
 *   <li>{@code purgeDays}：sys_slow_request 保留天数（默认 30）</li>
 *   <li>{@code alertWindowMinutes}：告警 Job 扫描的时间窗口（默认 10min）</li>
 *   <li>{@code alertRecipients}：告警发给哪些用户（用户名逗号分隔；默认 admin）</li>
 *   <li>{@code excludeUriPattern}：哪些 URI 不录入（默认 /actuator/.* 与 /swagger.*）</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "app.observability")
public class ObservabilityProperties
{
    private boolean enabled = true;
    private long slowMs = 2000L;
    private boolean recordClientError = false;
    private int purgeDays = 30;
    private int alertWindowMinutes = 10;
    private String alertRecipients = "admin";
    private String excludeUriPattern = "^/(actuator|swagger-ui|v3/api-docs|webjars|favicon|css|js|fonts).*";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public long getSlowMs() { return slowMs; }
    public void setSlowMs(long slowMs) { this.slowMs = slowMs; }
    public boolean isRecordClientError() { return recordClientError; }
    public void setRecordClientError(boolean recordClientError) { this.recordClientError = recordClientError; }
    public int getPurgeDays() { return purgeDays; }
    public void setPurgeDays(int purgeDays) { this.purgeDays = purgeDays; }
    public int getAlertWindowMinutes() { return alertWindowMinutes; }
    public void setAlertWindowMinutes(int alertWindowMinutes) { this.alertWindowMinutes = alertWindowMinutes; }
    public String getAlertRecipients() { return alertRecipients; }
    public void setAlertRecipients(String alertRecipients) { this.alertRecipients = alertRecipients; }
    public String getExcludeUriPattern() { return excludeUriPattern; }
    public void setExcludeUriPattern(String excludeUriPattern) { this.excludeUriPattern = excludeUriPattern; }
}
