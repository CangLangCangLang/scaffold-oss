package com.scaffold.framework.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Q-3 BusinessMetricsBinder 单测：
 * <ul>
 *   <li>启动后只为已存在的表注册 Gauge，缺表跳过</li>
 *   <li>refresh() 把 SELECT COUNT(*) 的结果同步到 Gauge</li>
 *   <li>查表抛异常 → debug 日志，不抛出</li>
 *   <li>trackedTables() 只返回真正注册过的表</li>
 * </ul>
 *
 * <p>不启动 Spring Context，直接用 reflect 注入 mocked JdbcTemplate（避免连接 DB）。
 */
class BusinessMetricsBinderTest
{
    private BusinessMetricsBinder binder;
    private org.springframework.jdbc.core.JdbcTemplate jdbc;
    private MeterRegistry registry;

    @BeforeEach
    void setUp()
    {
        // BusinessMetricsBinder 构造里要求一个 DataSource —— 用 mock 占位，紧接着把
        // 内部 jdbc 字段替换成可控的 mock JdbcTemplate
        DataSource ds = mock(DataSource.class, RETURNS_DEEP_STUBS);
        registry = new SimpleMeterRegistry();
        binder = new BusinessMetricsBinder(registry, ds);
        jdbc = mock(org.springframework.jdbc.core.JdbcTemplate.class);
        ReflectionTestUtils.setField(binder, "jdbc", jdbc);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void registersOnlyExistingTables()
    {
        when(jdbc.queryForList(anyString(), eq(String.class)))
                .thenReturn(List.of("cms_article", "form_template", "sys_message_inbox"));
        when(jdbc.queryForObject(anyString(), eq(Long.class)))
                .thenReturn(42L);

        binder.registerOnReady();

        assertThat(binder.trackedTables())
                .containsExactlyInAnyOrder("cms_article", "form_template", "sys_message_inbox");
        // Micrometer 已注册 3 个 Gauge
        assertThat(registry.find("scaffold.business.rows").gauges()).hasSize(3);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void refreshUpdatesGaugeValues()
    {
        when(jdbc.queryForList(anyString(), eq(String.class)))
                .thenReturn(List.of("cms_article"));
        when(jdbc.queryForObject(anyString(), eq(Long.class)))
                .thenReturn(7L)
                .thenReturn(15L);

        binder.registerOnReady();
        // registerOnReady 内部已经 refresh 一次，第二次手动再 refresh
        binder.refresh();

        double v = registry.find("scaffold.business.rows")
                .tag("table", "cms_article").gauge().value();
        assertThat(v).isEqualTo(15.0);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void countQueryFailureIsSwallowed()
    {
        when(jdbc.queryForList(anyString(), eq(String.class)))
                .thenReturn(List.of("cms_article"));
        when(jdbc.queryForObject(anyString(), eq(Long.class)))
                .thenThrow(new RuntimeException("table missing"));

        binder.registerOnReady();
        // 不抛
        binder.refresh();

        // gauge 仍为 0（默认值）
        double v = registry.find("scaffold.business.rows")
                .tag("table", "cms_article").gauge().value();
        assertThat(v).isEqualTo(0.0);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void noExistingTablesRegistersNothing()
    {
        when(jdbc.queryForList(anyString(), eq(String.class)))
                .thenReturn(List.of());

        binder.registerOnReady();

        assertThat(binder.trackedTables()).isEmpty();
        assertThat(registry.find("scaffold.business.rows").gauges()).isEmpty();
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void schemaQueryFailureFallsBackToEmptyList()
    {
        when(jdbc.queryForList(anyString(), eq(String.class)))
                .thenThrow(new RuntimeException("information_schema 不可访问"));

        binder.registerOnReady();

        assertThat(binder.trackedTables()).isEmpty();
    }
}
