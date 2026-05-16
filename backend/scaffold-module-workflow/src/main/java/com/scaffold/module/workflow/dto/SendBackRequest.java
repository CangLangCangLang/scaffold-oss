package com.scaffold.module.workflow.dto;

/**
 * 退回请求：把当前 task 跳到目标 activity（默认上一个 userTask）。
 *
 * @author scaffold
 */
public class SendBackRequest
{
    /** 目标 activity id；为空时自动取历史上"最近一次完成的 userTask" */
    private String targetActivityId;

    /** 退回理由（必填，进 task comment） */
    private String comment;

    public String getTargetActivityId() { return targetActivityId; }
    public void setTargetActivityId(String v) { this.targetActivityId = v; }

    public String getComment() { return comment; }
    public void setComment(String v) { this.comment = v; }
}
