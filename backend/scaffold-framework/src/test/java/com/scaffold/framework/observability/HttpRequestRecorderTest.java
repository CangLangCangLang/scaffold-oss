package com.scaffold.framework.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import com.scaffold.framework.observability.domain.SlowRequest;

/**
 * Q-3 HttpRequestRecorder 单测：覆盖 7 个场景：
 * <ul>
 *   <li>禁用 → 不录入</li>
 *   <li>正常快请求（< slowMs，2xx） → 不录入</li>
 *   <li>慢请求 → 录入 reason=SLOW</li>
 *   <li>5xx 响应 → 录入 reason=SERVER_ERROR</li>
 *   <li>4xx 响应 + recordClientError=true → 录入 reason=CLIENT_ERROR</li>
 *   <li>4xx 响应 + recordClientError=false → 不录入</li>
 *   <li>excludeUriPattern 命中（actuator） → 不录入</li>
 * </ul>
 */
class HttpRequestRecorderTest
{
    private SlowRequestPersistService persist;
    private ObservabilityProperties props;
    private HttpRequestRecorder recorder;

    @BeforeEach
    void setUp()
    {
        persist = mock(SlowRequestPersistService.class);
        props = new ObservabilityProperties();
        props.setEnabled(true);
        props.setSlowMs(1000);
        props.setRecordClientError(false);
        props.setExcludeUriPattern("^/actuator/.*$");
        recorder = new HttpRequestRecorder(props, persist);
    }

    @Test
    void disabledRecordsNothing() throws Exception
    {
        props.setEnabled(false);
        runFilter("/api/x", "GET", 200, 5000);
        verify(persist, never()).asyncSave(any());
    }

    @Test
    void fastRequestIsNotRecorded() throws Exception
    {
        runFilter("/api/x", "GET", 200, 50);
        verify(persist, never()).asyncSave(any());
    }

    @Test
    void slowRequestRecordedWithReasonSlow() throws Exception
    {
        runFilter("/api/x", "GET", 200, 1500);
        ArgumentCaptor<SlowRequest> cap = ArgumentCaptor.forClass(SlowRequest.class);
        verify(persist, times(1)).asyncSave(cap.capture());
        SlowRequest r = cap.getValue();
        assertThat(r.getReason()).isEqualTo(SlowRequest.REASON_SLOW);
        assertThat(r.getStatus()).isEqualTo(200);
        assertThat(r.getCostMs()).isGreaterThanOrEqualTo(1500);
        assertThat(r.getRequestUri()).isEqualTo("/api/x");
        assertThat(r.getAlerted()).isEqualTo(SlowRequest.ALERTED_NO);
    }

    @Test
    void serverErrorRecordedAsServerError() throws Exception
    {
        runFilter("/api/y", "POST", 500, 100);
        ArgumentCaptor<SlowRequest> cap = ArgumentCaptor.forClass(SlowRequest.class);
        verify(persist, times(1)).asyncSave(cap.capture());
        assertThat(cap.getValue().getReason()).isEqualTo(SlowRequest.REASON_SERVER_ERROR);
        assertThat(cap.getValue().getStatus()).isEqualTo(500);
    }

    @Test
    void clientErrorOnlyRecordedWhenFlagOn() throws Exception
    {
        // 默认 recordClientError=false：4xx 不录
        runFilter("/api/z", "GET", 404, 50);
        verify(persist, never()).asyncSave(any());

        // 开启后：4xx 录入
        props.setRecordClientError(true);
        runFilter("/api/z", "GET", 404, 50);
        ArgumentCaptor<SlowRequest> cap = ArgumentCaptor.forClass(SlowRequest.class);
        verify(persist, times(1)).asyncSave(cap.capture());
        assertThat(cap.getValue().getReason()).isEqualTo(SlowRequest.REASON_CLIENT_ERROR);
    }

    @Test
    void excludedUriIsSkipped() throws Exception
    {
        runFilter("/actuator/health", "GET", 200, 5000);
        verify(persist, never()).asyncSave(any());
    }

    @Test
    void uriIsTruncatedTo500Chars()
    {
        StringBuilder sb = new StringBuilder("/api/");
        sb.append("a".repeat(800));
        String safe = HttpRequestRecorder.safeUri(sb.toString());
        assertThat(safe).hasSize(500);
        assertThat(HttpRequestRecorder.safeUri(null)).isEmpty();
        assertThat(HttpRequestRecorder.safeUri("/api/short")).isEqualTo("/api/short");
    }

    @Test
    void exceptionMessageIsTruncated()
    {
        String long500 = "x".repeat(800);
        assertThat(HttpRequestRecorder.truncate(long500, HttpRequestRecorder.MAX_EXCEPTION_MSG_LENGTH))
                .hasSize(HttpRequestRecorder.MAX_EXCEPTION_MSG_LENGTH);
        assertThat(HttpRequestRecorder.truncate(null, 10)).isNull();
        assertThat(HttpRequestRecorder.truncate("hi", 10)).isEqualTo("hi");
    }

    private void runFilter(String uri, String method, int status, long sleepMs) throws Exception
    {
        MockHttpServletRequest req = new MockHttpServletRequest(method, uri);
        req.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        resp.setStatus(status);
        FilterChain chain = (request, response) ->
        {
            try
            {
                Thread.sleep(sleepMs);
            }
            catch (InterruptedException ignore)
            {
                Thread.currentThread().interrupt();
            }
        };
        recorder.doFilter(req, resp, chain);
    }
}
