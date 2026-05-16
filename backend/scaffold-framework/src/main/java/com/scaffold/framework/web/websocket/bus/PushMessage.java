package com.scaffold.framework.web.websocket.bus;

import java.io.Serializable;
import java.time.Instant;

/**
 * 推送消息封装。{@code id} 用于客户端去重；{@code type} 表示业务类型。
 * Jackson 默认按字段序列化，这里加 getter 兼容老反射工具。
 *
 * @author scaffold
 */
public class PushMessage implements Serializable
{
    private static final long serialVersionUID = 1L;

    /**
     * 投递维度。
     */
    public enum Scope
    {
        USER, // 点对点：toUser
        TOPIC // 主题广播：toTopic
    }

    private Scope scope;
    /** scope=USER 时为用户名（与 LoginUser.username 对齐）；scope=TOPIC 时为主题名（不带 /topic 前缀） */
    private String target;
    private String type;
    private String id;
    private Object payload;
    private Long timestamp;

    public PushMessage()
    {
    }

    public PushMessage(Scope scope, String target, String type, String id, Object payload)
    {
        this.scope = scope;
        this.target = target;
        this.type = type;
        this.id = id;
        this.payload = payload;
        this.timestamp = Instant.now().toEpochMilli();
    }

    public static PushMessage toUser(String username, String type, String id, Object payload)
    {
        return new PushMessage(Scope.USER, username, type, id, payload);
    }

    public static PushMessage toTopic(String topic, String type, String id, Object payload)
    {
        return new PushMessage(Scope.TOPIC, topic, type, id, payload);
    }

    public Scope getScope() { return scope; }
    public void setScope(Scope scope) { this.scope = scope; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Object getPayload() { return payload; }
    public void setPayload(Object payload) { this.payload = payload; }
    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
}
