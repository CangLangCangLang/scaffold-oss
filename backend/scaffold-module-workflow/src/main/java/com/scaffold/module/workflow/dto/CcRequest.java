package com.scaffold.module.workflow.dto;

import java.util.Date;
import java.util.List;

/**
 * 抄送请求：把流程的当前进展告知一组用户/角色，不阻塞流程，不形成 task 节点。
 * 接收方会通过 MessagePublisher.toUser 收到站内信（如果开启 inbox 模块还会落库）。
 *
 * @author scaffold
 */
public class CcRequest
{
    /** 抄送给的用户 id 列表（必填，至少 1 个） */
    private List<String> receiverUserIds;

    /** 抄送说明，进站内信内容 */
    private String comment;

    /** 客户端补充：抄送时间，仅用于审计与流程变量记录，缺省服务器侧填 */
    private Date occurredAt;

    public List<String> getReceiverUserIds() { return receiverUserIds; }
    public void setReceiverUserIds(List<String> v) { this.receiverUserIds = v; }

    public String getComment() { return comment; }
    public void setComment(String v) { this.comment = v; }

    public Date getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Date v) { this.occurredAt = v; }
}
