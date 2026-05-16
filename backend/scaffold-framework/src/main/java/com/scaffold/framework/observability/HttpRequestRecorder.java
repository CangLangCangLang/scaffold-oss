package com.scaffold.framework.observability;

import java.io.IOException;
import java.util.Date;
import java.util.regex.Pattern;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.scaffold.common.core.trace.TraceContext;
import com.scaffold.common.utils.SecurityUtils;
import com.scaffold.framework.observability.domain.SlowRequest;

/**
 * HTTP 请求耗时录入：
 *
 * <ul>
 *   <li>所有走过本 Filter 的请求都通过 Micrometer 默认 {@code http.server.requests} 自动埋点</li>
 *   <li>耗时 ≥ {@link ObservabilityProperties#getSlowMs()} 或响应 5xx → 异步落 sys_slow_request</li>
 *   <li>Filter 自身不依赖 Micrometer — 命中阈值才落表，不影响快路径</li>
 *   <li>排除路径走 {@link ObservabilityProperties#getExcludeUriPattern()}（默认排掉 actuator / swagger / 静态资源）</li>
 * </ul>
 *
 * <p>用 {@code @Lazy} 避免与 mapper 初始化形成循环（filter 会被 SecurityFilterChain 提早注册）。
 */
@Component
public class HttpRequestRecorder extends OncePerRequestFilter implements Ordered
{
    private static final Logger log = LoggerFactory.getLogger(HttpRequestRecorder.class);

    /** 异常摘要写入 sys_slow_request.exception_msg 的最大长度（DDL 是 VARCHAR(500)） */
    static final int MAX_EXCEPTION_MSG_LENGTH = 500;

    private final ObservabilityProperties props;
    private final SlowRequestPersistService persist;
    private volatile Pattern excludePattern;
    private volatile String excludeSource;

    public HttpRequestRecorder(ObservabilityProperties props, SlowRequestPersistService persist)
    {
        this.props = props;
        this.persist = persist;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException
    {
        if (!props.isEnabled() || isExcluded(request.getRequestURI()))
        {
            chain.doFilter(request, response);
            return;
        }

        long start = System.currentTimeMillis();
        Throwable thrown = null;
        try
        {
            chain.doFilter(request, response);
        }
        catch (Throwable t)
        {
            thrown = t;
            throw t;
        }
        finally
        {
            long cost = System.currentTimeMillis() - start;
            recordIfNeeded(request, response, cost, thrown);
        }
    }

    private boolean isExcluded(String uri)
    {
        String src = props.getExcludeUriPattern();
        if (src == null || src.isEmpty()) return false;
        Pattern p = excludePattern;
        if (p == null || !src.equals(excludeSource))
        {
            try
            {
                p = Pattern.compile(src);
                excludePattern = p;
                excludeSource = src;
            }
            catch (Exception e)
            {
                log.warn("[Observability] excludeUriPattern 非法 / pattern={}：{}", src, e.getMessage());
                return false;
            }
        }
        return uri != null && p.matcher(uri).matches();
    }

    private void recordIfNeeded(HttpServletRequest request, HttpServletResponse response, long cost, Throwable thrown)
    {
        int status = response.getStatus();
        boolean is5xx = status >= 500 || thrown != null;
        boolean is4xx = status >= 400 && status < 500;
        boolean slow = cost >= props.getSlowMs();
        if (!is5xx && !slow && !(is4xx && props.isRecordClientError()))
        {
            return;
        }

        String reason = is5xx ? SlowRequest.REASON_SERVER_ERROR
                : (slow ? SlowRequest.REASON_SLOW : SlowRequest.REASON_CLIENT_ERROR);
        SlowRequest r = new SlowRequest();
        r.setRequestUri(safeUri(request.getRequestURI()));
        r.setMethod(request.getMethod());
        r.setStatus(thrown != null && status < 500 ? 500 : status);
        r.setCostMs(cost);
        r.setTraceId(TraceContext.getTraceId());
        r.setUsername(extractUsername());
        r.setClientIp(extractClientIp(request));
        r.setReason(reason);
        r.setExceptionMsg(thrown == null ? null : truncate(thrown.toString(), MAX_EXCEPTION_MSG_LENGTH));
        r.setAlerted(SlowRequest.ALERTED_NO);
        r.setCreateTime(new Date());
        persist.asyncSave(r);
    }

    static String safeUri(String uri)
    {
        if (uri == null) return "";
        // 截断防止异常长 URI 把 VARCHAR(500) 撑炸（例如恶意构造）
        return uri.length() > 500 ? uri.substring(0, 500) : uri;
    }

    static String truncate(String s, int max)
    {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }

    private String extractUsername()
    {
        try
        {
            return SecurityUtils.getUsername();
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private String extractClientIp(HttpServletRequest req)
    {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip))
        {
            int comma = ip.indexOf(',');
            if (comma > 0) ip = ip.substring(0, comma);
            return ip.trim();
        }
        ip = req.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty()) return ip;
        return req.getRemoteAddr();
    }

    @Override
    public int getOrder()
    {
        // 优先级低于 TraceIdFilter（它要先把 traceId 放进 MDC 我们才能读到）
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
