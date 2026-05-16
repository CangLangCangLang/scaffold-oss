package com.scaffold.module.report.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.scaffold.common.annotation.AuditLog;
import com.scaffold.common.core.controller.BaseController;
import com.scaffold.common.core.domain.AjaxResult;
import com.scaffold.common.core.page.TableDataInfo;
import com.scaffold.common.exception.ServiceException;
import com.scaffold.common.utils.SecurityUtils;
import com.scaffold.module.report.domain.SysReportTemplate;
import com.scaffold.module.report.dto.RunRequest;
import com.scaffold.module.report.dto.RunResult;
import com.scaffold.module.report.mapper.SysReportTemplateMapper;
import com.scaffold.module.report.service.RunService;
import com.scaffold.module.report.util.ReportExporter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 报表运行 + 导出 + 运行日志（M-8）。
 *
 * <h3>权限</h3>
 * <ol>
 *   <li>必须具有 {@code report:template:run}（基础权限）</li>
 *   <li>若模板有 perm_key，再二次校验当前用户是否拥有 perm_key</li>
 *   <li>即席 SQL（不传 templateId）需额外 {@code report:template:add} 才能跑（管理员专用）</li>
 * </ol>
 *
 * @author scaffold
 */
@Tag(name = "报表中心 - 运行 / 导出（M-8）",
        description = "运行模板（行数 + 超时 + 权限三闸） + CSV/xlsx 导出 + 运行日志查看")
@RestController
@RequestMapping("/report/run")
public class RunController extends BaseController
{
    @Autowired
    private RunService runService;

    @Autowired
    private SysReportTemplateMapper templateMapper;

    @Value("${app.module.report.export-row-limit:50000}")
    private int exportRowLimit;

    @Operation(summary = "运行模板 / 即席 SQL，返回 JSON 结果")
    @PreAuthorize("@ss.hasPermi('report:template:run')")
    @AuditLog(module = "report.run", action = "RUN", resourceType = "template",
            resourceId = "#req.templateId",
            comment = "'运行报表 templateId=' + (#req.templateId != null ? #req.templateId : 'AD-HOC')")
    @PostMapping
    public AjaxResult run(@RequestBody RunRequest req)
    {
        ensurePermission(req);
        RunResult result = runService.run(req);
        AjaxResult ajax = success();
        ajax.put("data", result);
        return ajax;
    }

    @Operation(summary = "导出 CSV / xlsx",
            description = "format=csv|xlsx；超过 export-row-limit（默认 50000）的会被提示截断")
    @PreAuthorize("@ss.hasPermi('report:template:export')")
    @AuditLog(module = "report.run", action = "EXPORT", resourceType = "template",
            resourceId = "#req.templateId",
            comment = "'导出 ' + #format + ' templateId=' + (#req.templateId != null ? #req.templateId : 'AD-HOC')")
    @PostMapping("/export")
    public void export(@RequestBody RunRequest req,
                       @Parameter(description = "导出格式：csv / xlsx") @RequestParam("format") String format,
                       HttpServletResponse response) throws IOException
    {
        ensurePermission(req);
        Integer originalRowLimit = req.getRowLimit();
        req.setRowLimit(originalRowLimit == null ? exportRowLimit : Math.min(originalRowLimit, exportRowLimit));
        RunResult result = runService.run(req);

        String fmt = format == null ? "csv" : format.toLowerCase();
        String fileName = "report-"
                + (req.getTemplateId() == null ? "adhoc" : req.getTemplateId())
                + "-" + System.currentTimeMillis()
                + "." + ("xlsx".equals(fmt) ? "xlsx" : "csv");
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
        if ("xlsx".equals(fmt))
        {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            ReportExporter.writeXlsx(result, response.getOutputStream());
        }
        else
        {
            response.setContentType("text/csv;charset=utf-8");
            ReportExporter.writeCsv(result, response.getOutputStream());
        }
    }

    @Operation(summary = "运行日志分页（看自己的；管理员看全量）")
    @PreAuthorize("@ss.hasPermi('report:log:list')")
    @GetMapping("/log")
    public TableDataInfo log(@RequestParam(required = false) Long templateId,
                             @RequestParam(required = false) String status,
                             @RequestParam(required = false, defaultValue = "1") int pageNum,
                             @RequestParam(required = false, defaultValue = "10") int pageSize)
    {
        boolean canSeeAll = SecurityUtils.hasPermi("report:log:list:all");
        String createBy = canSeeAll ? null : SecurityUtils.getUsername();
        TableDataInfo r = new TableDataInfo();
        r.setRows(runService.page(templateId, createBy, status, pageNum, pageSize));
        r.setTotal(runService.total(templateId, createBy, status));
        r.setCode(200);
        r.setMsg("查询成功");
        return r;
    }

    @Operation(summary = "管理员立即触发运行日志清理")
    @PreAuthorize("@ss.hasPermi('report:log:list')")
    @PostMapping("/log/purge-now")
    public AjaxResult purgeNow(@RequestParam(defaultValue = "90") int days)
    {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.add(java.util.Calendar.DAY_OF_MONTH, -Math.max(1, days));
        int n = runService.purgeOlderThan(c.getTime());
        return success("清理 " + n + " 条 " + days + " 天前的运行日志");
    }

    /**
     * 二次权限校验：模板自带 perm_key + 即席 SQL 走管理员才能跑。
     */
    private void ensurePermission(RunRequest req)
    {
        if (req.getTemplateId() != null && req.getTemplateId() > 0)
        {
            SysReportTemplate t = templateMapper.selectById(req.getTemplateId());
            if (t == null) throw new ServiceException("模板不存在");
            if (t.getPermKey() != null && !t.getPermKey().isEmpty())
            {
                if (!SecurityUtils.hasPermi(t.getPermKey()))
                {
                    throw new ServiceException("无权限运行此模板（需要 " + t.getPermKey() + ")");
                }
            }
        }
        else
        {
            // 即席 SQL：要求 add 权限（同 template:add）
            if (!SecurityUtils.hasPermi("report:template:add"))
            {
                throw new ServiceException("即席 SQL 仅管理员可运行（需要 report:template:add）");
            }
        }
    }
}
