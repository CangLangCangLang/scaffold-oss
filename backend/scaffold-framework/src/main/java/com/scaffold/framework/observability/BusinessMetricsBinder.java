package com.scaffold.framework.observability;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

/**
 * 业务表行数 Gauge —— 启动后扫描 information_schema 看哪些约定的"业务主表"在当前库里：
 *
 * <ul>
 *   <li>cms_article（CMS 文章）/ cms_channel（CMS 栏目）</li>
 *   <li>form_template（M-10 表单模板）/ form_submission（M-10 提交记录）</li>
 *   <li>sys_file（M-6 文件）/ sys_file_share（M-6 分享）</li>
 *   <li>sys_report_template（M-8 模板）/ sys_report_run_log（M-8 运行历史）</li>
 *   <li>crm_customer / crm_lead / crm_opportunity / crm_contract（M-11）</li>
 *   <li>sys_audit_log（审计） / sys_message_inbox（站内信）</li>
 *   <li>sys_slow_request（Q-3 自身记录）</li>
 * </ul>
 *
 * <p>每张表注册一个 Gauge 名为 {@code scaffold.business.rows} 带 tag {@code table=<name>}。
 * 表不存在 → 自动跳过；values 60 秒刷一次（{@code @Scheduled} 单线程，不影响业务）。
 *
 * <p>对未来新增模块友好：在 {@link #TRACKED_TABLES} 加一行即可，不需要侵入业务模块。
 */
@Component
public class BusinessMetricsBinder
{
    private static final Logger log = LoggerFactory.getLogger(BusinessMetricsBinder.class);

    /** 60s 刷新一次（@Scheduled cron） */
    static final long REFRESH_INTERVAL_MS = 60_000L;

    /** 内置追踪的表清单（按业务模块分组） */
    private static final String[] TRACKED_TABLES = {
            // CMS（M-3）
            "cms_article", "cms_channel",
            // form-engine（M-10）
            "form_template", "form_submission",
            // file（M-6）
            "sys_file", "sys_file_share",
            // report（M-8）
            "sys_report_template", "sys_report_run_log",
            // CRM（M-11）
            "crm_customer", "crm_lead", "crm_opportunity", "crm_contract",
            // 平台
            "sys_audit_log", "sys_message_inbox", "sys_slow_request"
    };

    private final MeterRegistry meterRegistry;
    private final JdbcTemplate jdbc;
    /** table → AtomicLong（被 Micrometer Gauge 引用） */
    private final Map<String, AtomicLong> values = new LinkedHashMap<>();

    public BusinessMetricsBinder(MeterRegistry meterRegistry,
                                 @Qualifier("dynamicDataSource") DataSource dataSource)
    {
        this.meterRegistry = meterRegistry;
        this.jdbc = new JdbcTemplate(dataSource);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerOnReady()
    {
        List<String> existing = listExistingTables();
        for (String table : TRACKED_TABLES)
        {
            if (!existing.contains(table)) continue;
            AtomicLong holder = new AtomicLong(0);
            values.put(table, holder);
            Gauge.builder("scaffold.business.rows", holder, AtomicLong::doubleValue)
                    .description("业务主表行数（缓存 60s 刷新）")
                    .tags(Tags.of("table", table))
                    .register(meterRegistry);
        }
        if (!values.isEmpty())
        {
            log.info("[Observability] 已注册业务指标 Gauge: {} 张表", values.size());
            refresh();
        }
    }

    @Scheduled(fixedRate = REFRESH_INTERVAL_MS)
    public void refresh()
    {
        for (Map.Entry<String, AtomicLong> e : values.entrySet())
        {
            try
            {
                Long n = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM " + e.getKey(), Long.class);
                e.getValue().set(n == null ? 0 : n);
            }
            catch (Exception ex)
            {
                log.debug("[Observability] {} 计数失败：{}", e.getKey(), ex.getMessage());
            }
        }
    }

    private List<String> listExistingTables()
    {
        try
        {
            return jdbc.queryForList(
                    "SELECT TABLE_NAME FROM information_schema.TABLES "
                            + "WHERE TABLE_SCHEMA = DATABASE()", String.class);
        }
        catch (Exception e)
        {
            log.warn("[Observability] 扫表清单失败：{}", e.getMessage());
            return List.of();
        }
    }

    /** 测试 / 自检用：当前已注册哪些 table */
    public List<String> trackedTables()
    {
        return List.copyOf(values.keySet());
    }
}
