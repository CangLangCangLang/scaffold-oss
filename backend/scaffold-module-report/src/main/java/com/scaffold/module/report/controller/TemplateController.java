package com.scaffold.module.report.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.scaffold.common.annotation.AuditLog;
import com.scaffold.common.core.controller.BaseController;
import com.scaffold.common.core.domain.AjaxResult;
import com.scaffold.common.core.page.TableDataInfo;
import com.scaffold.module.report.domain.SysReportTemplate;
import com.scaffold.module.report.dto.TemplateQuery;
import com.scaffold.module.report.service.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 报表模板 CRUD（M-8）。
 *
 * @author scaffold
 */
@Tag(name = "报表中心 - 模板（M-8）", description = "SQL 模板 CRUD（保存时强制 SqlGuard 校验 select-only）")
@RestController
@RequestMapping("/report/template")
public class TemplateController extends BaseController
{
    @Autowired
    private TemplateService templateService;

    @Operation(summary = "分页查询模板")
    @PreAuthorize("@ss.hasPermi('report:template:list')")
    @GetMapping
    public TableDataInfo list(@ModelAttribute TemplateQuery q)
    {
        TableDataInfo r = new TableDataInfo();
        r.setRows(templateService.page(q));
        r.setTotal(templateService.total(q));
        r.setCode(200);
        r.setMsg("查询成功");
        return r;
    }

    @Operation(summary = "查看模板详情")
    @PreAuthorize("@ss.hasPermi('report:template:list')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id)
    {
        return success(templateService.detail(id));
    }

    @Operation(summary = "新增模板")
    @PreAuthorize("@ss.hasPermi('report:template:add')")
    @AuditLog(module = "report.template", action = "ADD", resourceType = "template",
            resourceId = "#result?.data",
            comment = "'新增模板 ' + #t.code")
    @PostMapping
    public AjaxResult add(@RequestBody SysReportTemplate t)
    {
        t.setId(null);
        return success(templateService.save(t));
    }

    @Operation(summary = "编辑模板")
    @PreAuthorize("@ss.hasPermi('report:template:edit')")
    @AuditLog(module = "report.template", action = "EDIT", resourceType = "template",
            resourceId = "#t.id",
            comment = "'编辑模板 ' + #t.id")
    @PutMapping
    public AjaxResult edit(@RequestBody SysReportTemplate t)
    {
        return success(templateService.save(t));
    }

    @Operation(summary = "删除模板")
    @PreAuthorize("@ss.hasPermi('report:template:remove')")
    @AuditLog(module = "report.template", action = "REMOVE", resourceType = "template",
            resourceId = "#id",
            comment = "'删除模板 ' + #id")
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id)
    {
        templateService.remove(id);
        return success();
    }

    @Operation(summary = "校验模板 SQL（仅做 SqlGuard 检查，不入库）")
    @PreAuthorize("@ss.hasPermi('report:template:add')")
    @PostMapping("/validate")
    public AjaxResult validate(@RequestBody SysReportTemplate t)
    {
        templateService.validateAndDefault(t);
        return success("校验通过");
    }
}
