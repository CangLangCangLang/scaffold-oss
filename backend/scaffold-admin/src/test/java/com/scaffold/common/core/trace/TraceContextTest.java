package com.scaffold.common.core.trace;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import com.scaffold.common.core.domain.R;

class TraceContextTest
{
    @AfterEach
    void cleanup()
    {
        TraceContext.clear();
    }

    @Test
    void generateProducesNonEmptyId()
    {
        assertThat(TraceContext.generate()).hasSize(32);
    }

    @Test
    void getTraceIdReturnsEmptyWhenNotSet()
    {
        assertThat(TraceContext.getTraceId()).isEmpty();
    }

    @Test
    void setTraceIdPropagatesToResponse()
    {
        TraceContext.setTraceId("trace-123");
        assertThat(TraceContext.getTraceId()).isEqualTo("trace-123");

        R<String> r = R.ok("payload");
        assertThat(r.getTraceId()).isEqualTo("trace-123");
    }

    @Test
    void clearRemovesTraceId()
    {
        TraceContext.setTraceId("abc");
        TraceContext.clear();
        assertThat(TraceContext.getTraceId()).isEmpty();
    }
}
