package com.scaffold.module.inbox.domain;

import java.util.Date;

/**
 * 离线消息盒记录（与 sys_message_inbox 一一对应）。
 *
 * @author scaffold
 */
public class MessageInboxEntry
{
    private Long id;
    private String messageId;
    private String scope;
    private String target;
    private String type;
    private String payload;
    /** 0=未读 1=已读 2=已过期 */
    private Integer status;
    private Date createdAt;
    private Date readAt;
    private Date expireAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getReadAt() { return readAt; }
    public void setReadAt(Date readAt) { this.readAt = readAt; }
    public Date getExpireAt() { return expireAt; }
    public void setExpireAt(Date expireAt) { this.expireAt = expireAt; }
}
