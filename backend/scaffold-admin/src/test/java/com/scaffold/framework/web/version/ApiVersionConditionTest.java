package com.scaffold.framework.web.version;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ApiVersionConditionTest
{
    @Test
    void requestMatchesWhenVersionGreaterOrEqual()
    {
        ApiVersionCondition condition = new ApiVersionCondition(2);
        HttpServletRequest req = new MockHttpServletRequest("GET", "/api/v3/order/list");
        assertThat(condition.getMatchingCondition(req)).isNotNull();
    }

    @Test
    void requestDoesNotMatchWhenVersionLower()
    {
        ApiVersionCondition condition = new ApiVersionCondition(2);
        HttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/order/list");
        assertThat(condition.getMatchingCondition(req)).isNull();
    }

    @Test
    void combinePrefersOtherCondition()
    {
        ApiVersionCondition base = new ApiVersionCondition(1);
        ApiVersionCondition method = new ApiVersionCondition(3);
        assertThat(base.combine(method).getApiVersion()).isEqualTo(3);
    }

    @Test
    void compareToFavorsHigherVersion()
    {
        ApiVersionCondition v1 = new ApiVersionCondition(1);
        ApiVersionCondition v3 = new ApiVersionCondition(3);
        // 与 RequestCondition 习惯一致：返回负数表示 v3 优先
        assertThat(v3.compareTo(v1, new MockHttpServletRequest())).isLessThan(0);
        assertThat(v1.compareTo(v3, new MockHttpServletRequest())).isGreaterThan(0);
    }
}
