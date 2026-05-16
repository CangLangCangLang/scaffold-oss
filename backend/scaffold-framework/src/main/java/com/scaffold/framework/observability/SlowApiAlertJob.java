package com.scaffold.framework.observability;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import com.scaffold.framework.observability.domain.SlowRequest;
import com.scaffold.framework.observability.mapper.SlowRequestMapper;
import com.scaffold.framework.web.websocket.bus.MessagePublisher;

/**
 * Quartz：扫上窗口（默认 10min）的 sys_slow_request alerted=0 → 给管理员发 inbox。
 *
 * <p>触发：sys_job 9001（cron 表达式 0 _slash_5 * * * ?，每 5 分钟）调 {@code scanAndAlert()}；
 * 单次触发可以处理多条慢/错误请求，按 reason 聚合，每条 reason 一条站内信。
 *
 * <p>type 命名：
 * <ul>
 *   <li>{@code observability.slow_request} — 慢请求批次</li>
 *   <li>{@code observability.server_error} — 5xx 批次</li>
 * </ul>
 *
 * <p>对 MessagePublisher 用 {@link ObjectProvider} 软依赖：framework 自带桥但允许外部禁用。
 */
@Component("slowApiAlertJob")
public class SlowApiAlertJob
{
    private static final Logger log = LoggerFactory.getLogger(SlowApiAlertJob.class);

    public static final String TYPE_SLOW = "observability.slow_request";
    public static final String TYPE_SERVER_ERROR = "observability.server_error";
    /** 单条 inbox 最多塞几个样本（防 payload 过大） */
    static final int MAX_SAMPLES_PER_NOTIFICATION = 5;

    private final ObservabilityProperties props;
    private final SlowRequestMapper mapper;
    private final ObjectProvider<MessagePublisher> publisherProvider;

    public SlowApiAlertJob(ObservabilityProperties props, SlowRequestMapper mapper,
                           ObjectProvider<MessagePublisher> publisherProvider)
    {
        this.props = props;
        this.mapper = mapper;
        this.publisherProvider = publisherProvider;
    }

    /** Quartz 入口：扫 + 发 + 标记 */
    public int scanAndAlert()
    {
        if (!props.isEnabled())
        {
            return 0;
        }
        Date since = new Date(System.currentTimeMillis() - props.getAlertWindowMinutes() * 60_000L);
        List<SlowRequest> pending;
        try
        {
            pending = mapper.selectPendingAlerts(since);
        }
        catch (Exception e)
        {
            log.warn("[Observability] 查 pending alerts 失败：{}", e.getMessage());
            return 0;
        }
        if (pending.isEmpty()) return 0;

        MessagePublisher publisher = publisherProvider.getIfAvailable();
        if (publisher == null)
        {
            log.warn("[Observability] MessagePublisher 不可用，{} 条告警仅落表不推 inbox", pending.size());
            // 仍然要 mark — 不然下轮 Job 永远重复
            markAlertedSafely(pending);
            return pending.size();
        }

        // 按 reason 分组（SLOW vs SERVER_ERROR），不同主题分别发
        Map<String, List<SlowRequest>> grouped = new HashMap<>();
        for (SlowRequest r : pending)
        {
            grouped.computeIfAbsent(r.getReason(), k -> new ArrayList<>()).add(r);
        }
        List<String> recipients = parseRecipients(props.getAlertRecipients());
        for (Map.Entry<String, List<SlowRequest>> e : grouped.entrySet())
        {
            String type = SlowRequest.REASON_SLOW.equals(e.getKey()) ? TYPE_SLOW : TYPE_SERVER_ERROR;
            Map<String, Object> payload = buildPayload(e.getKey(), e.getValue());
            for (String recipient : recipients)
            {
                try
                {
                    publisher.toUser(recipient, type, payload);
                }
                catch (Exception ex)
                {
                    log.warn("[Observability] 发 inbox 给 {} 失败 type={}：{}",
                            recipient, type, ex.getMessage());
                }
            }
        }
        markAlertedSafely(pending);
        log.info("[Observability] 告警发送完毕 reason 分组={} pending={}", grouped.size(), pending.size());
        return pending.size();
    }

    private void markAlertedSafely(List<SlowRequest> pending)
    {
        try
        {
            mapper.markAlerted(pending.stream().map(SlowRequest::getId).toList());
        }
        catch (Exception ex)
        {
            log.warn("[Observability] markAlerted 失败：{}", ex.getMessage());
        }
    }

    static Map<String, Object> buildPayload(String reason, List<SlowRequest> records)
    {
        Map<String, Object> payload = new HashMap<>();
        payload.put("reason", reason);
        payload.put("count", records.size());
        // 取耗时最长的前 N 条作为样本
        List<Map<String, Object>> samples = records.stream()
                .sorted((a, b) -> Long.compare(b.getCostMs(), a.getCostMs()))
                .limit(MAX_SAMPLES_PER_NOTIFICATION)
                .map(SlowApiAlertJob::sampleOf)
                .toList();
        payload.put("samples", samples);
        payload.put("link", "/observability/slow-request");
        return payload;
    }

    private static Map<String, Object> sampleOf(SlowRequest r)
    {
        Map<String, Object> s = new HashMap<>();
        s.put("uri", r.getRequestUri());
        s.put("method", r.getMethod());
        s.put("status", r.getStatus());
        s.put("costMs", r.getCostMs());
        s.put("traceId", r.getTraceId());
        s.put("username", r.getUsername());
        s.put("createTime", r.getCreateTime());
        return s;
    }

    static List<String> parseRecipients(String csv)
    {
        if (csv == null || csv.isBlank()) return List.of("admin");
        List<String> out = new ArrayList<>();
        for (String s : csv.split(","))
        {
            String t = s.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out.isEmpty() ? List.of("admin") : out;
    }
}
