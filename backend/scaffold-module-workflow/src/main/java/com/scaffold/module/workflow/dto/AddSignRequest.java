package com.scaffold.module.workflow.dto;

/**
 * 后加签请求：在当前 task 完成之后，给指定用户再开一个相同节点的任务。
 * 实现方式：完成当前任务前给变量 scaffoldAddSignAfter 追加一个 (taskId, addedAssignee)，
 * 由 taskService.complete 之后再 createTaskBuilder() 创建新任务。
 *
 * 前加签见 {@link AddSignBeforeRequest}。
 *
 * @author scaffold
 */
public class AddSignRequest
{
    /** 加签给谁（必填） */
    private String assignee;

    /** 加签说明 */
    private String comment;

    public String getAssignee() { return assignee; }
    public void setAssignee(String v) { this.assignee = v; }

    public String getComment() { return comment; }
    public void setComment(String v) { this.comment = v; }
}
