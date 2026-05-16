package com.scaffold.module.form.controller;

import java.util.Map;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.scaffold.common.annotation.AuditLog;
import com.scaffold.common.core.controller.BaseController;
import com.scaffold.common.core.domain.AjaxResult;
import com.scaffold.common.core.page.TableDataInfo;
import com.scaffold.module.form.domain.FormTemplate;
import com.scaffold.module.form.dto.FormTemplateQuery;
import com.scaffold.module.form.dto.FormTemplateSaveRequest;
import com.scaffold.module.form.service.FormTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 表单模板 CRUD + 状态机端点（M-10）。
 *
 * @author scaffold
 */
@Tag(name = "表单模板（M-10）", description = "通用表单模板库：草稿 / 发布 / 归档；schemaJson 来自 form-create 设计器")
@RestController
@RequestMapping("/form/template")
public class FormTemplateController extends BaseController
{
    @Autowired private FormTemplateService templateService;

    @Operation(summary = "分页查询模板列表")
    @PreAuthorize("@ss.hasPermi('form:template:list')")
    @GetMapping
    public TableDataInfo list(@ModelAttribute FormTemplateQuery q)
    {
        Map<String, Object> p = templateService.page(q);
        TableDataInfo info = new TableDataInfo();
        info.setRows((java.util.List<?>) p.get("rows"));
        info.setTotal((long) p.get("total"));
        info.setCode(200);
        info.setMsg("查询成功");
        return info;
    }

    @Operation(summary = "查看模板详情")
    @PreAuthorize("@ss.hasPermi('form:template:list')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id)
    {
        return success(templateService.detail(id));
    }

    @Operation(summary = "拿 formKey 当前激活（PUBLISHED）的版本", description = "给前端填报页用；查不到返 null")
    @PreAuthorize("@ss.hasPermi('form:submission:add')")
    @GetMapping("/active")
    public AjaxResult active(@Parameter(description = "模板 key") @RequestParam String formKey)
    {
        FormTemplate t = templateService.activeByKey(formKey);
        return success(t);
    }

    @Operation(summary = "新增模板（DRAFT）")
    @PreAuthorize("@ss.hasPermi('form:template:add')")
    @AuditLog(module = "form.template", action = "CREATE", resourceType = "form_template",
            resourceId = "#result?.data?.id",
            comment = "'新增表单模板 ' + #req.formKey")
    @PostMapping
    public AjaxResult add(@RequestBody FormTemplateSaveRequest req)
    {
        req.setId(null);
        return success(templateService.save(req));
    }

    @Operation(summary = "编辑模板",
            description = "草稿原地改；已发布 / 归档自动派生 version+1 的新草稿，不破坏在线版本")
    @PreAuthorize("@ss.hasPermi('form:template:edit')")
    @AuditLog(module = "form.template", action = "EDIT", resourceType = "form_template",
            resourceId = "#req.id",
            comment = "'编辑表单模板 ' + #req.id")
    @PutMapping
    public AjaxResult edit(@RequestBody FormTemplateSaveRequest req)
    {
        return success(templateService.save(req));
    }

    @Operation(summary = "发布模板（DRAFT → PUBLISHED）")
    @PreAuthorize("@ss.hasPermi('form:template:publish')")
    @AuditLog(module = "form.template", action = "PUBLISH", resourceType = "form_template",
            resourceId = "#id",
            comment = "'发布表单模板 ' + #id")
    @PostMapping("/{id}/publish")
    public AjaxResult publish(@PathVariable Long id)
    {
        return success(templateService.publish(id));
    }

    @Operation(summary = "归档模板（PUBLISHED → ARCHIVED）")
    @PreAuthorize("@ss.hasPermi('form:template:publish')")
    @AuditLog(module = "form.template", action = "ARCHIVE", resourceType = "form_template",
            resourceId = "#id",
            comment = "'归档表单模板 ' + #id")
    @PostMapping("/{id}/archive")
    public AjaxResult archive(@PathVariable Long id)
    {
        return success(templateService.archive(id));
    }

    @Operation(summary = "软删模板（草稿 / 归档可删；已发布需先归档）")
    @PreAuthorize("@ss.hasPermi('form:template:remove')")
    @AuditLog(module = "form.template", action = "DELETE", resourceType = "form_template",
            resourceId = "#id",
            comment = "'软删表单模板 ' + #id",
            recordReturn = false)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id)
    {
        templateService.remove(id);
        return success();
    }
}
