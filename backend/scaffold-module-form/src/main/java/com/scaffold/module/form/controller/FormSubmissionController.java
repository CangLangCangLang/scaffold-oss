package com.scaffold.module.form.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.scaffold.common.annotation.AuditLog;
import com.scaffold.common.core.controller.BaseController;
import com.scaffold.common.core.domain.AjaxResult;
import com.scaffold.common.core.page.TableDataInfo;
import com.scaffold.module.form.dto.FormSubmissionQuery;
import com.scaffold.module.form.dto.FormSubmissionRequest;
import com.scaffold.module.form.service.FormSubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 表单提交记录端点（M-10）。
 *
 * <p>非 admin 列表 / 详情自动按 submitter=current 过滤；admin 看全量。
 *
 * @author scaffold
 */
@Tag(name = "表单提交记录（M-10）", description = "表单填报落地：混合存储模型，按 submitter / template 隔离")
@RestController
@RequestMapping("/form/submission")
public class FormSubmissionController extends BaseController
{
    @Autowired private FormSubmissionService submissionService;

    @Operation(summary = "提交表单（创建一条 form_submission 记录）")
    @PreAuthorize("@ss.hasPermi('form:submission:add')")
    @AuditLog(module = "form.submission", action = "SUBMIT", resourceType = "form_submission",
            resourceId = "#result?.data?.id",
            comment = "'提交表单 templateId=' + #req.templateId")
    @PostMapping
    public AjaxResult submit(@RequestBody FormSubmissionRequest req)
    {
        return success(submissionService.submit(req));
    }

    @Operation(summary = "分页查询提交记录（admin 全量；非 admin 仅自己的）")
    @PreAuthorize("@ss.hasPermi('form:submission:list')")
    @GetMapping
    public TableDataInfo list(@ModelAttribute FormSubmissionQuery q)
    {
        Map<String, Object> p = submissionService.page(q);
        TableDataInfo info = new TableDataInfo();
        info.setRows((java.util.List<?>) p.get("rows"));
        info.setTotal((long) p.get("total"));
        info.setCode(200);
        info.setMsg("查询成功");
        return info;
    }

    @Operation(summary = "查看提交记录详情（admin 任意；非 admin 仅自己的）")
    @PreAuthorize("@ss.hasPermi('form:submission:list')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id)
    {
        return success(submissionService.detail(id));
    }
}
