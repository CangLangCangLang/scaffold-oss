package com.scaffold.framework.filter;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.scaffold.common.core.trace.TraceContext;
import com.scaffold.common.utils.StringUtils;

/**
 * 请求级 TraceId 过滤器。
 * <p>
 * 优先使用上游传入的 {@code X-Trace-Id}，否则生成新的 32 位无连字符 UUID。
 * 写入 MDC 供日志输出，回写响应头便于前端联调和链路追踪。
 *
 * @author scaffold
 */
@Component
public class TraceIdFilter extends OncePerRequestFilter implements Ordered
{
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException
    {
        String traceId = request.getHeader(TraceContext.HEADER_NAME);
        if (StringUtils.isEmpty(traceId))
        {
            traceId = TraceContext.generate();
        }
        try
        {
            TraceContext.setTraceId(traceId);
            response.setHeader(TraceContext.HEADER_NAME, traceId);
            filterChain.doFilter(request, response);
        }
        finally
        {
            TraceContext.clear();
        }
    }

    @Override
    public int getOrder()
    {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
