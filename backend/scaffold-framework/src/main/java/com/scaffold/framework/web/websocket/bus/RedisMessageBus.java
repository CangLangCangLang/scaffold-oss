package com.scaffold.framework.web.websocket.bus;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * 基于 Redis Pub/Sub 的多实例推送总线：
 * <ol>
 *   <li>业务侧调用 {@link #toUser} / {@link #toTopic}，把 {@link PushMessage} 发到 Redis 频道</li>
 *   <li>每个应用实例订阅相同频道（见 {@link RedisMessageBusConfig}），收到后通过本地
 *       {@link SimpMessagingTemplate} 投递到本节点上挂着的 STOMP 会话</li>
 * </ol>
 * 这样无须 sticky session 也能在多实例下保证消息送达正确的连接。
 *
 * @author scaffold
 */
@Component
public class RedisMessageBus implements MessagePublisher
{
    private static final Logger log = LoggerFactory.getLogger(RedisMessageBus.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final String channel;
    /**
     * 可选记录器（如 inbox 模块的 {@code MessageInboxService}）。
     * <p>
     * framework 只依赖 {@link MessageBusRecorder} 接口，不强依赖任何业务模块；
     * 当业务模块没引入时 {@link ObjectProvider} 为空，跳过记录这一步。
     */
    private final ObjectProvider<MessageBusRecorder> recorderProvider;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public RedisMessageBus(RedisTemplate redisTemplate,
            SimpMessagingTemplate messagingTemplate,
            ObjectProvider<MessageBusRecorder> recorderProvider,
            @Value("${websocket.bus.channel:scaffold:ws:bus}") String channel)
    {
        this.redisTemplate = (RedisTemplate<String, Object>) redisTemplate;
        this.messagingTemplate = messagingTemplate;
        this.recorderProvider = recorderProvider;
        this.channel = channel;
    }

    public String getChannel()
    {
        return channel;
    }

    @Override
    public void toUser(String username, String type, Object payload)
    {
        toUser(username, type, UUID.randomUUID().toString(), payload);
    }

    @Override
    public void toUser(String username, String type, String messageId, Object payload)
    {
        publish(PushMessage.toUser(username, type, messageId, payload));
    }

    @Override
    public void toTopic(String topic, String type, Object payload)
    {
        toTopic(topic, type, UUID.randomUUID().toString(), payload);
    }

    @Override
    public void toTopic(String topic, String type, String messageId, Object payload)
    {
        publish(PushMessage.toTopic(topic, type, messageId, payload));
    }

    private void publish(PushMessage message)
    {
        // 1. 让可选记录器先落库（如 inbox 模块）。Redis 故障时仍能保留消息。
        try
        {
            MessageBusRecorder recorder = recorderProvider.getIfAvailable();
            if (recorder != null) recorder.record(message);
        }
        catch (Exception e)
        {
            log.warn("MessageBusRecorder 处理失败 type={} reason={}", message.getType(), e.getMessage());
        }
        // 2. fan-out
        try
        {
            redisTemplate.convertAndSend(channel, message);
        }
        catch (Exception e)
        {
            log.warn("Redis 总线发布失败，降级为本地投递 reason={}", e.getMessage());
            deliverLocal(message);
        }
    }

    /**
     * 由 {@link RedisMessageBusConfig} 注册的 Listener 在收到 Redis 消息后回调，
     * 在本节点把消息分发给挂在 STOMP 上的会话。
     */
    public void deliverLocal(PushMessage message)
    {
        if (message == null) return;
        try
        {
            switch (message.getScope())
            {
                case USER -> messagingTemplate.convertAndSendToUser(
                        message.getTarget(), "/queue/notice", message);
                case TOPIC -> messagingTemplate.convertAndSend(
                        "/topic/" + message.getTarget(), message);
            }
        }
        catch (Exception e)
        {
            log.warn("STOMP 本地投递失败 target={} type={} reason={}",
                    message.getTarget(), message.getType(), e.getMessage());
        }
    }
}
