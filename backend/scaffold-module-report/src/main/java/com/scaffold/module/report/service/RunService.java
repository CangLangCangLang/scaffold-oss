package com.scaffold.module.report.service;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.common.exception.ServiceException;
import com.scaffold.common.utils.SecurityUtils;
import com.scaffold.module.report.domain.SysReportRunLog;
import com.scaffold.module.report.domain.SysReportTemplate;
import com.scaffold.module.report.dto.RunRequest;
import com.scaffold.module.report.dto.RunResult;
import com.scaffold.module.report.mapper.SysReportRunLogMapper;
import com.scaffold.module.report.mapper.SysReportTemplateMapper;
import com.scaffold.module.report.runtime.ReportRunner;
import com.scaffold.module.report.security.ReportSqlGuard;

/**
 * 报表运行 + 写运行日志（成功 / 失败 / 超时三类都落）。
 *
 * <h3>权限校验</h3>
 * 模板 perm_key 在 controller 层用 @PreAuthorize 复合校验；service 层只看登录态。
 *
 * @author scaffold
 */
@Service
public class RunService
{
    private static final Logger log = LoggerFactory.getLogger(RunService.class);

    private final ObjectMapper json = new ObjectMapper();

    @Autowired
    private SysReportTemplateMapper templateMapper;

    @Autowired
    private SysReportRunLogMapper runLogMapper;

    @Autowired
    private ReportRunner runner;

    /**
     * 运行模板（templateId 优先）或即席 SQL（仅特权用户走，由 controller 控）。
     *
     * <p>不论成功 / 失败，落一条 sys_report_run_log。</p>
     */
    public RunResult run(RunRequest req)
    {
        if (req == null) throw new ServiceException("req 不能为空");

        SysReportTemplate template = null;
        Long datasourceId;
        String sqlText;
        Integer rowLimit;
        Integer timeoutMs;
        String templateCode = null;

        if (req.getTemplateId() != null && req.getTemplateId() > 0)
        {
            template = templateMapper.selectById(req.getTemplateId());
            if (template == null) throw new ServiceException("模板不存在");
            if (!"0".equals(template.getStatus())) throw new ServiceException("模板已停用");
            datasourceId = template.getDatasourceId();
            sqlText = template.getSqlText();
            rowLimit = pickMin(req.getRowLimit(), template.getRowLimit());
            timeoutMs = pickMin(req.getTimeoutMs(), template.getTimeoutMs());
            templateCode = template.getCode();
        }
        else
        {
            if (req.getSql() == null || req.getSql().isEmpty())
            {
                throw new ServiceException("templateId 与 sql 必填其一");
            }
            ReportSqlGuard.ensureSelectOnly(req.getSql());
            datasourceId = req.getDatasourceId() == null ? 0L : req.getDatasourceId();
            sqlText = req.getSql();
            rowLimit = req.getRowLimit();
            timeoutMs = req.getTimeoutMs();
        }

        SysReportRunLog logRow = new SysReportRunLog();
        logRow.setTemplateId(template == null ? null : template.getId());
        logRow.setTemplateCode(templateCode);
        logRow.setDatasourceId(datasourceId == null ? 0L : datasourceId);
        logRow.setParamJson(toJson(req.getParams()));
        logRow.setCreateBy(SecurityUtils.getUsername());
        logRow.setCreateTime(new Date());

        try
        {
            RunResult result = runner.execute(datasourceId, sqlText, req.getParams(), rowLimit, timeoutMs);
            logRow.setSqlPreview(result.getSqlPreview());
            logRow.setRowCount(result.getRowCount());
            logRow.setCostMs(result.getCostMs());
            logRow.setStatus("0");
            saveLog(logRow);
            result.setRunLogId(logRow.getId());
            return result;
        }
        catch (ServiceException e)
        {
            logRow.setSqlPreview(safeSqlPreview(sqlText));
            logRow.setRowCount(0);
            logRow.setCostMs(null);
            String msg = e.getMessage();
            logRow.setStatus(msg != null && msg.contains("超时") ? "2" : "1");
            logRow.setErrorMsg(truncate(msg, 1990));
            saveLog(logRow);
            throw e;
        }
        catch (Exception e)
        {
            log.error("[report-run-error] template={} ds={}", template == null ? null : template.getCode(), datasourceId, e);
            logRow.setSqlPreview(safeSqlPreview(sqlText));
            logRow.setRowCount(0);
            logRow.setStatus("1");
            logRow.setErrorMsg(truncate(e.getClass().getSimpleName() + ": " + e.getMessage(), 1990));
            saveLog(logRow);
            throw new ServiceException("查询失败：" + e.getMessage());
        }
    }

    public List<SysReportRunLog> page(Long templateId, String createBy, String status,
                                      int pageNum, int pageSize)
    {
        int pn = pageNum <= 0 ? 1 : pageNum;
        int ps = pageSize <= 0 ? 10 : Math.min(pageSize, 200);
        return runLogMapper.selectPage(templateId, createBy, status, (pn - 1) * ps, ps);
    }

    public long total(Long templateId, String createBy, String status)
    {
        return runLogMapper.count(templateId, createBy, status);
    }

    public int purgeOlderThan(Date threshold)
    {
        return runLogMapper.deleteOlderThan(threshold);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void saveLog(SysReportRunLog row)
    {
        try
        {
            runLogMapper.insert(row);
        }
        catch (Exception e)
        {
            log.warn("[report-runlog-fail] {}", e.getMessage());
        }
    }

    private static Integer pickMin(Integer a, Integer b)
    {
        if (a == null) return b;
        if (b == null) return a;
        return Math.min(a, b);
    }

    private String toJson(Map<String, Object> map)
    {
        if (map == null || map.isEmpty()) return null;
        Map<String, Object> ordered = new LinkedHashMap<>(map);
        try
        {
            return json.writeValueAsString(ordered);
        }
        catch (JsonProcessingException e)
        {
            return ordered.toString();
        }
    }

    private static String safeSqlPreview(String sql)
    {
        if (sql == null) return "";
        return sql.length() > 1900 ? sql.substring(0, 1900) + "..." : sql;
    }

    private static String truncate(String s, int max)
    {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
