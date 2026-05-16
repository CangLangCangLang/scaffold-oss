package com.scaffold.web.controller.system;

import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.scaffold.common.core.controller.BaseController;
import com.scaffold.common.core.domain.AjaxResult;
import com.scaffold.framework.web.websocket.bus.MessagePublisher;

/**
 * 推送总线 REST 入口，便于内部任务 / 管理后台触发推送。
 *
 * @author scaffold
 */
@RestController
@RequestMapping("/system/message")
public class WebSocketPushController extends BaseController
{
    private final MessagePublisher publisher;

    public WebSocketPushController(MessagePublisher publisher)
    {
        this.publisher = publisher;
    }

    @PreAuthorize("@ss.hasPermi('system:message:push')")
    @PostMapping("/user")
    public AjaxResult pushUser(@Validated @RequestBody UserPushRequest req)
    {
        publisher.toUser(req.getUsername(), req.getType(), req.getMessageId(), req.getPayload());
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('system:message:push')")
    @PostMapping("/topic")
    public AjaxResult pushTopic(@Validated @RequestBody TopicPushRequest req)
    {
        publisher.toTopic(req.getTopic(), req.getType(), req.getMessageId(), req.getPayload());
        return AjaxResult.success();
    }

    public static class UserPushRequest
    {
        @NotBlank
        private String username;
        @NotBlank
        private String type;
        private String messageId;
        private Object payload;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }
        public Object getPayload() { return payload; }
        public void setPayload(Object payload) { this.payload = payload; }
    }

    public static class TopicPushRequest
    {
        @NotBlank
        private String topic;
        @NotBlank
        private String type;
        private String messageId;
        private Object payload;

        public String getTopic() { return topic; }
        public void setTopic(String topic) { this.topic = topic; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }
        public Object getPayload() { return payload; }
        public void setPayload(Object payload) { this.payload = payload; }
    }
}
