package com.scaffold.module.report.controller;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
import com.scaffold.module.report.domain.SysReportDashboard;
import com.scaffold.module.report.domain.SysReportDashboardCard;
import com.scaffold.module.report.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 看板 CRUD。查看时若看板有 perm_key，需同时具备此 key + report:dashboard:view。
 *
 * @author scaffold
 */
@Tag(name = "报表中心 - 看板（M-8）",
        description = "看板与卡片 CRUD（保存时整批替换卡片）；查看走 perm_key 二次校验")
@RestController
@RequestMapping("/report/dashboard")
public class DashboardController extends BaseController
{
    @Autowired
    private DashboardService dashboardService;

    @Operation(summary = "看板分页")
    @PreAuthorize("@ss.hasPermi('report:dashboard:list')")
    @GetMapping
    public TableDataInfo list(@RequestParam(required = false) String name,
                              @RequestParam(required = false) String category,
                              @RequestParam(required = false) String status,
                              @RequestParam(required = false, defaultValue = "1") int pageNum,
                              @RequestParam(required = false, defaultValue = "10") int pageSize)
    {
        TableDataInfo r = new TableDataInfo();
        r.setRows(dashboardService.page(name, category, status, pageNum, pageSize));
        r.setTotal(dashboardService.total(name, category, status));
        r.setCode(200);
        r.setMsg("查询成功");
        return r;
    }

    @Operation(summary = "看板详情（含卡片）")
    @PreAuthorize("@ss.hasPermi('report:dashboard:view')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id)
    {
        Map<String, Object> data = dashboardService.detail(id);
        SysReportDashboard d = (SysReportDashboard) data.get("dashboard");
        if (d.getPermKey() != null && !d.getPermKey().isEmpty()
                && !SecurityUtils.hasPermi(d.getPermKey()))
        {
            throw new ServiceException("无权限查看此看板（需要 " + d.getPermKey() + ")");
        }
        return success(data);
    }

    @Operation(summary = "新增看板（含卡片）")
    @PreAuthorize("@ss.hasPermi('report:dashboard:add')")
    @AuditLog(module = "report.dashboard", action = "ADD", resourceType = "dashboard",
            resourceId = "#result?.data",
            comment = "'新增看板 ' + #req.dashboard.code")
    @PostMapping
    public AjaxResult add(@RequestBody DashboardSaveRequest req)
    {
        if (req == null || req.getDashboard() == null) throw new ServiceException("参数缺失");
        req.getDashboard().setId(null);
        return success(dashboardService.save(req.getDashboard(), req.getCards()));
    }

    @Operation(summary = "编辑看板（含卡片，整批替换）")
    @PreAuthorize("@ss.hasPermi('report:dashboard:edit')")
    @AuditLog(module = "report.dashboard", action = "EDIT", resourceType = "dashboard",
            resourceId = "#req.dashboard.id",
            comment = "'编辑看板 ' + #req.dashboard.id")
    @PutMapping
    public AjaxResult edit(@RequestBody DashboardSaveRequest req)
    {
        if (req == null || req.getDashboard() == null || req.getDashboard().getId() == null)
        {
            throw new ServiceException("参数缺失");
        }
        return success(dashboardService.save(req.getDashboard(), req.getCards()));
    }

    @Operation(summary = "删除看板")
    @PreAuthorize("@ss.hasPermi('report:dashboard:remove')")
    @AuditLog(module = "report.dashboard", action = "REMOVE", resourceType = "dashboard",
            resourceId = "#id",
            comment = "'删除看板 ' + #id")
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id)
    {
        dashboardService.remove(id);
        return success();
    }

    /** 兼容请求体：dashboard + cards 一次性提交 */
    public static class DashboardSaveRequest
    {
        private SysReportDashboard dashboard;
        private List<SysReportDashboardCard> cards;
        public SysReportDashboard getDashboard() { return dashboard; }
        public void setDashboard(SysReportDashboard dashboard) { this.dashboard = dashboard; }
        public List<SysReportDashboardCard> getCards() { return cards; }
        public void setCards(List<SysReportDashboardCard> cards) { this.cards = cards; }
    }
}
