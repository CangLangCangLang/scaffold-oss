package com.scaffold.framework.observability.domain;

import java.util.Date;

/**
 * 慢请求 / 错误请求记录（Q-3 可观测性 sys_slow_request）。
 *
 * <p>{@link com.scaffold.framework.observability.HttpRequestRecorder} 写入；
 * {@code SlowApiAlertJob} 消费后批量更新 {@code alerted=1}。
 */
public class SlowRequest
{
    public static final String REASON_SLOW = "SLOW";
    public static final String REASON_SERVER_ERROR = "SERVER_ERROR";
    public static final String REASON_CLIENT_ERROR = "CLIENT_ERROR";

    public static final String ALERTED_NO = "0";
    public static final String ALERTED_YES = "1";

    private Long id;
    private String requestUri;
    private String method;
    private Integer status;
    private Long costMs;
    private String traceId;
    private String username;
    private String clientIp;
    private String reason;
    private String exceptionMsg;
    private String alerted;
    private Date createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRequestUri() { return requestUri; }
    public void setRequestUri(String requestUri) { this.requestUri = requestUri; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Long getCostMs() { return costMs; }
    public void setCostMs(Long costMs) { this.costMs = costMs; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getExceptionMsg() { return exceptionMsg; }
    public void setExceptionMsg(String exceptionMsg) { this.exceptionMsg = exceptionMsg; }
    public String getAlerted() { return alerted; }
    public void setAlerted(String alerted) { this.alerted = alerted; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
