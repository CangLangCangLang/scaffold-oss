package com.scaffold.framework.observability.controller;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.scaffold.common.core.domain.AjaxResult;
import com.scaffold.framework.observability.BusinessMetricsBinder;
import com.scaffold.framework.observability.SlowApiAlertJob;
import com.scaffold.framework.observability.domain.SlowRequest;
import com.scaffold.framework.observability.mapper.SlowRequestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Q-3 可观测性后台 API：
 * <ul>
 *   <li>GET /monitor/slow-request — 列表查询</li>
 *   <li>POST /monitor/slow-request/purge?days= — 手工清理 N 天前</li>
 *   <li>POST /monitor/slow-request/scan-now — 手工触发告警 Job</li>
 *   <li>GET /monitor/business-metrics — 当前已注册的业务指标 table 列表</li>
 *   <li>DELETE /monitor/slow-request/{id} — 单条删除</li>
 * </ul>
 *
 * <p>所有路径要求 monitor:* 权限（菜单 9001-9013 已绑给超管角色）。
 */
@Tag(name = "Q-3 可观测性", description = "慢请求记录 + 业务指标 + 健康检查后台接口")
@RestController
@RequestMapping("/monitor/slow-request")
public class SlowRequestController
{
    @Autowired
    private SlowRequestMapper mapper;

    @Autowired
    private SlowApiAlertJob alertJob;

    @Autowired
    private BusinessMetricsBinder metricsBinder;

    @Operation(summary = "列表查询慢/错误请求")
    @PreAuthorize("@ss.hasPermi('monitor:slow:list')")
    @GetMapping
    public AjaxResult list(
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) String requestUri,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date beginTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime)
    {
        List<SlowRequest> rows = mapper.selectList(reason, requestUri, beginTime, endTime);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("rows", rows);
        data.put("total", rows.size());
        data.put("pending", mapper.countPending());
        return AjaxResult.success(data);
    }

    @Operation(summary = "清理 N 天前慢请求记录")
    @PreAuthorize("@ss.hasPermi('monitor:slow:purge')")
    @PostMapping("/purge")
    public AjaxResult purge(@RequestParam(defaultValue = "30") int days)
    {
        int n = mapper.deleteOlderThan(days);
        return AjaxResult.success("已清理 " + n + " 条记录");
    }

    @Operation(summary = "立即触发告警扫描（运维手动）")
    @PreAuthorize("@ss.hasPermi('monitor:slow:purge')")
    @PostMapping("/scan-now")
    public AjaxResult scanNow()
    {
        int sent = alertJob.scanAndAlert();
        return AjaxResult.success("已扫描并发送 " + sent + " 条记录的告警");
    }

    @Operation(summary = "已注册的业务指标 Gauge table 列表")
    @PreAuthorize("@ss.hasPermi('monitor:metrics:view')")
    @GetMapping("/business-metrics")
    public AjaxResult businessMetrics()
    {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tables", metricsBinder.trackedTables());
        data.put("count", metricsBinder.trackedTables().size());
        data.put("hint", "实时数值见 /actuator/prometheus 中的 scaffold_business_rows{table=...}");
        return AjaxResult.success(data);
    }

    @Operation(summary = "单条删除")
    @PreAuthorize("@ss.hasPermi('monitor:slow:purge')")
    @DeleteMapping("/{id}")
    public AjaxResult delete(@org.springframework.web.bind.annotation.PathVariable Long id)
    {
        int n = mapper.deleteById(id);
        return n > 0 ? AjaxResult.success() : AjaxResult.error("记录不存在");
    }
}
