package com.scaffold.module.workflow.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.flowable.engine.repository.Deployment;
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
import org.springframework.web.multipart.MultipartFile;
import com.scaffold.common.annotation.AuditLog;
import com.scaffold.common.core.controller.BaseController;
import com.scaffold.common.core.domain.AjaxResult;
import com.scaffold.common.core.page.TableDataInfo;
import com.scaffold.common.exception.ServiceException;
import com.scaffold.common.utils.SecurityUtils;
import com.scaffold.module.workflow.dto.ProcessDefinitionView;
import com.scaffold.module.workflow.dto.ProcessInstanceView;
import com.scaffold.module.workflow.dto.ProcessRuntimeStateView;
import com.scaffold.module.workflow.dto.StartProcessRequest;
import com.scaffold.module.workflow.service.WorkflowFacade;

/**
 * 流程定义 / 实例管理。
 *
 * @author scaffold
 */
@RestController
@RequestMapping("/workflow/process")
public class WorkflowProcessController extends BaseController
{
    @Autowired
    private WorkflowFacade workflowFacade;

    @PreAuthorize("@ss.hasPermi('workflow:process:list')")
    @GetMapping("/definitions")
    public AjaxResult listDefinitions(@RequestParam(required = false) String keyword)
    {
        List<ProcessDefinitionView> list = workflowFacade.listLatestProcessDefinitions(keyword);
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('workflow:process:deploy')")
    @AuditLog(module = "workflow.process", action = "DEPLOY", resourceType = "deployment",
            resourceId = "#result?.data?.id",
            comment = "'部署 BPMN 流程: ' + (#name ?: #file?.originalFilename)")
    @PostMapping("/deployments")
    public AjaxResult deploy(@RequestParam(value = "name", required = false) String name,
                             @RequestParam("file") MultipartFile file) throws IOException
    {
        if (file == null || file.isEmpty()) throw new ServiceException("BPMN 文件不能为空");
        String filename = file.getOriginalFilename();
        if (filename == null || !(filename.endsWith(".bpmn") || filename.endsWith(".bpmn20.xml") || filename.endsWith(".xml")))
        {
            throw new ServiceException("仅支持 .bpmn / .bpmn20.xml / .xml 文件");
        }
        try (InputStream in = file.getInputStream())
        {
            Deployment d = workflowFacade.deployBpmn(name, filename, in);
            return AjaxResult.success("部署成功", java.util.Map.of(
                    "id", d.getId(),
                    "name", d.getName() == null ? filename : d.getName(),
                    "deploymentTime", d.getDeploymentTime()));
        }
    }

    @PreAuthorize("@ss.hasPermi('workflow:process:list')")
    @GetMapping("/definitions/{processDefinitionId}/xml")
    public AjaxResult getBpmnXml(@PathVariable String processDefinitionId) throws IOException
    {
        try (InputStream in = workflowFacade.getBpmnXml(processDefinitionId))
        {
            String xml = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            return AjaxResult.success(java.util.Map.of("xml", xml));
        }
    }

    /**
     * 列出某 processDefinitionKey 的所有历史版本（按 version 倒序），给前端"版本对比"下拉用。
     * 与 {@code GET /definitions} 不同 —— 后者只返每个 key 的最新激活版本，这里全量返回含 suspended。
     *
     * @param key processDefinitionKey，必填
     */
    @PreAuthorize("@ss.hasPermi('workflow:process:list')")
    @GetMapping("/definitions/by-key/{key}/versions")
    public AjaxResult listVersionsByKey(@PathVariable String key)
    {
        return AjaxResult.success(workflowFacade.listVersionsByKey(key));
    }

    @PreAuthorize("@ss.hasPermi('workflow:process:remove')")
    @AuditLog(module = "workflow.process", action = "DELETE_DEPLOYMENT",
            resourceType = "deployment", resourceId = "#deploymentId",
            comment = "'删除流程部署 ' + #deploymentId + ' (cascade=' + #cascade + ')'",
            recordReturn = false)
    @DeleteMapping("/deployments/{deploymentId}")
    public AjaxResult deleteDeployment(@PathVariable String deploymentId,
                                       @RequestParam(defaultValue = "true") boolean cascade)
    {
        workflowFacade.deleteDeployment(deploymentId, cascade);
        return AjaxResult.success();
    }

    // ---------------- Process Instance ----------------

    @PreAuthorize("@ss.hasPermi('workflow:process:start')")
    @AuditLog(module = "workflow.process", action = "START", resourceType = "processInstance",
            resourceId = "#result?.data?.id",
            comment = "'启动流程 ' + #request.processDefinitionKey")
    @PostMapping("/instances")
    public AjaxResult start(@RequestBody StartProcessRequest request)
    {
        String userId = userIdAsString();
        ProcessInstanceView view = workflowFacade.startProcess(request, userId);
        return AjaxResult.success(view);
    }

    @PreAuthorize("@ss.hasPermi('workflow:process:list')")
    @GetMapping("/instances/mine")
    public AjaxResult myInstances()
    {
        return AjaxResult.success(workflowFacade.listMyStartedActiveInstances(userIdAsString()));
    }

    /**
     * 流程实例总列表（admin 视角全量；非 admin 强制按 startedBy=current 过滤，不引入 dataScope，
     * 因为 ACT_HI_PROCINST 不接入 mybatis mapper.xml，无法套 ${params.dataScope}）。
     *
     * @param processDefinitionKey 精确匹配，可选
     * @param businessKey 精确匹配，可选
     * @param startUserId 精确匹配，可选；非 admin 时被强制覆盖为当前用户 id
     * @param status running / finished / all（默认 all）
     * @param pageNum 1-based，默认 1
     * @param pageSize 默认 20，最大 200
     */
    @PreAuthorize("@ss.hasPermi('workflow:process:list')")
    @GetMapping("/instances")
    public TableDataInfo searchInstances(
            @RequestParam(required = false) String processDefinitionKey,
            @RequestParam(required = false) String businessKey,
            @RequestParam(required = false) String startUserId,
            @RequestParam(required = false, defaultValue = "all") String status,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize)
    {
        String effectiveActor = SecurityUtils.isAdmin(getUserId()) ? startUserId : userIdAsString();
        java.util.Map<String, Object> result = workflowFacade.searchInstances(
                processDefinitionKey, businessKey, effectiveActor, status,
                pageNum == null ? 1 : pageNum, pageSize == null ? 20 : pageSize);
        TableDataInfo info = new TableDataInfo();
        info.setRows((java.util.List<?>) result.get("rows"));
        info.setTotal((long) result.get("total"));
        info.setCode(200);
        info.setMsg("查询成功");
        return info;
    }

    @PreAuthorize("@ss.hasPermi('workflow:process:cancel')")
    @AuditLog(module = "workflow.process", action = "CANCEL", resourceType = "processInstance",
            resourceId = "#processInstanceId",
            comment = "'取消流程实例 ' + #processInstanceId + ' 原因: ' + #reason",
            recordReturn = false)
    @DeleteMapping("/instances/{processInstanceId}")
    public AjaxResult cancel(@PathVariable String processInstanceId,
                             @RequestParam(required = false) String reason)
    {
        workflowFacade.cancelInstance(processInstanceId, reason == null ? "user_cancel" : reason);
        return AjaxResult.success();
    }

    /**
     * 流程实例的运行时态：active / completed / rejected 三组节点 id，
     * 配合 {@code BpmnDesigner readonly highlights=...} 在前端渲染流程图叠色。
     */
    @PreAuthorize("@ss.hasPermi('workflow:process:list')")
    @GetMapping("/instances/{processInstanceId}/state")
    public AjaxResult getInstanceState(@PathVariable String processInstanceId)
    {
        ProcessRuntimeStateView state = workflowFacade.getRuntimeState(processInstanceId);
        return AjaxResult.success(state);
    }

    /**
     * 流程实例下未完成的 task 列表。<br>
     * 给跨模块联动 / E2E 脚本 / 未来 ProcessProgressDialog 的"加签 / 撤回"按钮使用：
     * 业务方拿着 businessKey 拉到 piid 后，再调这个端点找具体的待办 task。
     */
    @PreAuthorize("@ss.hasPermi('workflow:process:list')")
    @GetMapping("/instances/{processInstanceId}/tasks")
    public AjaxResult listInstanceTasks(@PathVariable String processInstanceId)
    {
        return AjaxResult.success(workflowFacade.listActiveTasksByProcessInstance(processInstanceId));
    }

    /**
     * 流程实例的操作时间轴：包含节点到达 / 完成、抄送、加签（前 / 后）、退回、评论等事件，
     * 已按时间升序排好序，前端 ElTimeline 直接渲染即可。
     */
    @PreAuthorize("@ss.hasPermi('workflow:process:list')")
    @GetMapping("/instances/{processInstanceId}/timeline")
    public AjaxResult getInstanceTimeline(@PathVariable String processInstanceId)
    {
        return AjaxResult.success(workflowFacade.getInstanceTimeline(processInstanceId));
    }

    /**
     * 直接通过流程实例 id 拿对应的 BPMN XML——前端在"查看流程进度"按钮点开时少一次往返。
     */
    @PreAuthorize("@ss.hasPermi('workflow:process:list')")
    @GetMapping("/instances/{processInstanceId}/xml")
    public AjaxResult getInstanceBpmnXml(@PathVariable String processInstanceId) throws IOException
    {
        ProcessRuntimeStateView state = workflowFacade.getRuntimeState(processInstanceId);
        try (InputStream in = workflowFacade.getBpmnXml(state.getProcessDefinitionId()))
        {
            String xml = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            return AjaxResult.success(java.util.Map.of("xml", xml, "state", state));
        }
    }

    private String userIdAsString()
    {
        Long uid = getUserId();
        return uid == null ? null : String.valueOf(uid);
    }
}
