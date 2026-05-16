package com.scaffold.module.inbox.service;

import org.springframework.stereotype.Component;
import com.scaffold.framework.web.websocket.bus.MessageBusRecorder;
import com.scaffold.framework.web.websocket.bus.PushMessage;

/**
 * 把推送总线的 USER 消息记录到 inbox。
 * <p>
 * framework 通过 {@link MessageBusRecorder} 接口拿到本 Bean，做到"模块在则记录、模块不在则跳过"。
 *
 * @author scaffold
 */
@Component
public class InboxMessageBusRecorder implements MessageBusRecorder
{
    private final MessageInboxService inboxService;

    public InboxMessageBusRecorder(MessageInboxService inboxService)
    {
        this.inboxService = inboxService;
    }

    @Override
    public Long record(PushMessage message)
    {
        return inboxService.persistIfEnabled(message);
    }
}
