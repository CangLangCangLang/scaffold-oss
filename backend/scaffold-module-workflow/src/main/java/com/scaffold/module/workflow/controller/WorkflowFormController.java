package com.scaffold.module.workflow.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import com.scaffold.module.workflow.domain.WfFormSchema;
import com.scaffold.module.workflow.service.WfFormSchemaService;

/**
 * 工作流动态表单 schema 管理：流程定义级别保存表单模板，启动 / 办理时由前端拉取并渲染。
 * <p>
 * 当前 MVP 仅支持启动表单（activityId={@link WfFormSchema#ACTIVITY_START_FORM}），
 * 其他节点的表单 schema 也兼容存（按需在 BPMN 设计器里挂指定 activityId 即可）。
 *
 * @author scaffold
 */
@RestController
@RequestMapping("/workflow/form")
public class WorkflowFormController extends BaseController
{
    @Autowired
    private WfFormSchemaService formSchemaService;

    /**
     * 保存或新建版本：每次都新增一行 version+1 的记录并把旧版本置为 enabled=0。
     */
    @PreAuthorize("@ss.hasPermi('workflow:form:edit')")
    @AuditLog(module = "workflow.form", action = "SAVE", resourceType = "formSchema",
            resourceId = "#result?.data?.id",
            comment = "'保存表单 schema processKey=' + #req?.processDefinitionKey + ' activity=' + #req?.activityId")
    @PostMapping("/schemas")
    public AjaxResult save(@RequestBody WfFormSchema req)
    {
        WfFormSchema saved = formSchemaService.saveAsNewVersion(req, currentUserName());
        return AjaxResult.success(saved);
    }

    /** 获取启用中的最新 schema；找不到返回 200 + null（前端据此判断是否走默认裸 form）。 */
    @PreAuthorize("@ss.hasPermi('workflow:form:list')")
    @GetMapping("/schemas/active")
    public AjaxResult getActive(@RequestParam(required = false) String processDefinitionKey,
                                @RequestParam(required = false) String activityId)
    {
        WfFormSchema schema = formSchemaService.findActive(processDefinitionKey, activityId);
        return AjaxResult.success(schema);
    }

    @PreAuthorize("@ss.hasPermi('workflow:form:list')")
    @GetMapping("/schemas")
    public AjaxResult listByDef(@RequestParam(required = false) String processDefinitionKey)
    {
        List<WfFormSchema> list = formSchemaService.listByDefinitionKey(processDefinitionKey);
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('workflow:form:list')")
    @GetMapping("/schemas/{id}")
    public AjaxResult getById(@PathVariable Long id)
    {
        return AjaxResult.success(formSchemaService.findById(id));
    }

    @PreAuthorize("@ss.hasPermi('workflow:form:remove')")
    @AuditLog(module = "workflow.form", action = "DELETE", resourceType = "formSchema",
            resourceId = "#id", recordReturn = false)
    @DeleteMapping("/schemas/{id}")
    public AjaxResult delete(@PathVariable Long id)
    {
        formSchemaService.delete(id, currentUserName());
        return AjaxResult.success();
    }

    private String currentUserName()
    {
        try
        {
            return getUsername();
        }
        catch (Exception e)
        {
            return null;
        }
    }
}
