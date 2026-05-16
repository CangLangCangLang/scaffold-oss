package com.scaffold.module.workflow.dto;

/**
 * 前加签入参：在当前 task 之前并行插入一个新审批人；
 * 原任务被标记为阻塞，必须等加签人完成后才能提交。
 *
 * @author scaffold
 */
public class AddSignBeforeRequest
{
    /** 加签的目标用户名 / userId */
    private String assignee;
    /** 加签理由（可选） */
    private String comment;

    public String getAssignee() { return assignee; }
    public void setAssignee(String assignee) { this.assignee = assignee; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
