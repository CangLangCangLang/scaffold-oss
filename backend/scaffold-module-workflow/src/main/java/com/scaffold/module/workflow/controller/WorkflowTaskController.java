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
import com.scaffold.common.utils.SecurityUtils;
import com.scaffold.module.workflow.dto.AddSignBeforeRequest;
import com.scaffold.module.workflow.dto.AddSignRequest;
import com.scaffold.module.workflow.dto.CcRequest;
import com.scaffold.module.workflow.dto.CompleteTaskRequest;
import com.scaffold.module.workflow.dto.SendBackRequest;
import com.scaffold.module.workflow.dto.TaskView;
import com.scaffold.module.workflow.service.WorkflowFacade;

/**
 * 任务（待办 / 已办 / 完成 / 认领 / 转办）。
 *
 * @author scaffold
 */
@RestController
@RequestMapping("/workflow/task")
public class WorkflowTaskController extends BaseController
{
    @Autowired
    private WorkflowFacade workflowFacade;

    @PreAuthorize("@ss.hasPermi('workflow:task:list')")
    @GetMapping("/todo")
    public AjaxResult todo(@RequestParam(required = false) String keyword)
    {
        List<TaskView> list = workflowFacade.listTodoTasks(userIdAsString(), keyword);
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('workflow:task:list')")
    @GetMapping("/done")
    public AjaxResult done()
    {
        return AjaxResult.success(workflowFacade.listDoneTasks(userIdAsString()));
    }

    @PreAuthorize("@ss.hasPermi('workflow:task:complete')")
    @AuditLog(module = "workflow.task", action = "COMPLETE", resourceType = "task",
            resourceId = "#taskId",
            comment = "'完成任务 ' + #taskId + (#req?.comment != null ? ' 意见:' + #req.comment : '')",
            recordReturn = false)
    @PostMapping("/{taskId}/complete")
    public AjaxResult complete(@PathVariable String taskId, @RequestBody(required = false) CompleteTaskRequest req)
    {
        workflowFacade.completeTask(taskId, req, userIdAsString());
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('workflow:task:claim')")
    @AuditLog(module = "workflow.task", action = "CLAIM", resourceType = "task",
            resourceId = "#taskId",
            comment = "'认领任务 ' + #taskId", recordReturn = false)
    @PostMapping("/{taskId}/claim")
    public AjaxResult claim(@PathVariable String taskId)
    {
        workflowFacade.claimTask(taskId, userIdAsString());
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('workflow:task:claim')")
    @AuditLog(module = "workflow.task", action = "UNCLAIM", resourceType = "task",
            resourceId = "#taskId",
            comment = "'撤销认领任务 ' + #taskId", recordReturn = false)
    @PostMapping("/{taskId}/unclaim")
    public AjaxResult unclaim(@PathVariable String taskId)
    {
        workflowFacade.unclaimTask(taskId);
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('workflow:task:delegate')")
    @AuditLog(module = "workflow.task", action = "DELEGATE", resourceType = "task",
            resourceId = "#taskId",
            comment = "'转办任务 ' + #taskId + ' → ' + #targetUserId", recordReturn = false)
    @PostMapping("/{taskId}/delegate")
    public AjaxResult delegate(@PathVariable String taskId, @RequestParam String targetUserId)
    {
        workflowFacade.delegateTask(taskId, targetUserId);
        return AjaxResult.success();
    }

    /** 抄送：把任务进展告诉一组人，不阻塞流程。 */
    @PreAuthorize("@ss.hasPermi('workflow:task:cc')")
    @AuditLog(module = "workflow.task", action = "CC", resourceType = "task",
            resourceId = "#taskId",
            comment = "'抄送任务 ' + #taskId + ' → ' + #req?.receiverUserIds",
            recordReturn = false)
    @PostMapping("/{taskId}/cc")
    public AjaxResult cc(@PathVariable String taskId, @RequestBody CcRequest req)
    {
        workflowFacade.cc(taskId, req, userIdAsString());
        return AjaxResult.success();
    }

    /** 后加签：在当前 task 之后给指定用户再开一个相同节点的任务。 */
    @PreAuthorize("@ss.hasPermi('workflow:task:addsign')")
    @AuditLog(module = "workflow.task", action = "ADD_SIGN_AFTER", resourceType = "task",
            resourceId = "#taskId",
            comment = "'后加签 ' + #taskId + ' → ' + #req?.assignee",
            recordReturn = false)
    @PostMapping("/{taskId}/add-sign")
    public AjaxResult addSign(@PathVariable String taskId, @RequestBody AddSignRequest req)
    {
        workflowFacade.addSignAfter(taskId, req, userIdAsString());
        return AjaxResult.success();
    }

    /** 前加签：在当前 task 之前并行插入一个新审批人；原任务被阻塞直到加签人完成。 */
    @PreAuthorize("@ss.hasPermi('workflow:task:addsign-before')")
    @AuditLog(module = "workflow.task", action = "ADD_SIGN_BEFORE", resourceType = "task",
            resourceId = "#taskId",
            comment = "'前加签 ' + #taskId + ' → ' + #req?.assignee",
            recordReturn = false)
    @PostMapping("/{taskId}/add-sign-before")
    public AjaxResult addSignBefore(@PathVariable String taskId, @RequestBody AddSignBeforeRequest req)
    {
        workflowFacade.addSignBefore(taskId, req, userIdAsString());
        return AjaxResult.success();
    }

    /**
     * 撤销前加签：仅限子任务的前加签发起人或 admin。撤销后子任务被删，父任务的阻塞标记同步清掉。
     * @param childTaskId 前加签创建的子任务 id
     */
    @PreAuthorize("@ss.hasPermi('workflow:task:addsign-before')")
    @AuditLog(module = "workflow.task", action = "ADD_SIGN_BEFORE_CANCEL", resourceType = "task",
            resourceId = "#childTaskId",
            comment = "'撤销前加签 ' + #childTaskId",
            recordReturn = false)
    @DeleteMapping("/{childTaskId}/add-sign-before")
    public AjaxResult cancelAddSignBefore(@PathVariable String childTaskId)
    {
        workflowFacade.cancelPreSign(childTaskId, userIdAsString(), SecurityUtils.isAdmin(getUserId()));
        return AjaxResult.success();
    }

    /** 退回：把当前 task 跳到目标节点（默认上一 userTask）。 */
    @PreAuthorize("@ss.hasPermi('workflow:task:sendback')")
    @AuditLog(module = "workflow.task", action = "SEND_BACK", resourceType = "task",
            resourceId = "#taskId",
            comment = "'退回任务 ' + #taskId + ' → ' + (#req?.targetActivityId ?: '上一节点') + ' :: ' + #req?.comment",
            recordReturn = false)
    @PostMapping("/{taskId}/send-back")
    public AjaxResult sendBack(@PathVariable String taskId, @RequestBody SendBackRequest req)
    {
        workflowFacade.sendBack(taskId, req, userIdAsString());
        return AjaxResult.success();
    }

    private String userIdAsString()
    {
        Long uid = getUserId();
        return uid == null ? null : String.valueOf(uid);
    }
}
