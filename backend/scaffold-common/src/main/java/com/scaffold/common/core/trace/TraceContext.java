package com.scaffold.common.core.trace;

import java.util.UUID;
import org.slf4j.MDC;

/**
 * TraceId 上下文。
 * <p>
 * 使用 SLF4J MDC 在线程上下文中传递 traceId，便于在日志、响应头、统一返回体中贯通同一次请求。
 * 异步线程需要显式拷贝（参考 {@code MDC.getCopyOfContextMap()}）。
 *
 * @author scaffold
 */
public final class TraceContext
{
    public static final String MDC_KEY = "traceId";

    public static final String HEADER_NAME = "X-Trace-Id";

    private TraceContext()
    {
    }

    public static String generate()
    {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String getTraceId()
    {
        String traceId = MDC.get(MDC_KEY);
        return traceId == null ? "" : traceId;
    }

    public static void setTraceId(String traceId)
    {
        if (traceId != null && !traceId.isEmpty())
        {
            MDC.put(MDC_KEY, traceId);
        }
    }

    public static void clear()
    {
        MDC.remove(MDC_KEY);
    }
}
