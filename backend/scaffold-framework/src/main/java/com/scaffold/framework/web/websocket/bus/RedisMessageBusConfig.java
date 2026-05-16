package com.scaffold.framework.web.websocket.bus;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 把 {@link RedisMessageBus} 注册为 Redis Pub/Sub 订阅者。
 * 同一频道的消息无论由哪个节点发布，所有节点都会接收并通过 STOMP 投递。
 *
 * @author scaffold
 */
@Configuration
public class RedisMessageBusConfig
{
    @Bean
    public Jackson2JsonRedisSerializer<PushMessage> pushMessageSerializer(ObjectMapper objectMapper)
    {
        return new Jackson2JsonRedisSerializer<>(objectMapper, PushMessage.class);
    }

    @Bean
    public MessageListenerAdapter pushMessageListener(RedisMessageBus bus,
            Jackson2JsonRedisSerializer<PushMessage> serializer)
    {
        MessageListenerAdapter adapter = new MessageListenerAdapter(bus, "deliverLocal");
        adapter.setSerializer(serializer);
        return adapter;
    }

    @Bean
    public RedisMessageListenerContainer pushMessageContainer(RedisConnectionFactory connectionFactory,
            MessageListenerAdapter adapter, RedisMessageBus bus)
    {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(adapter, new PatternTopic(bus.getChannel()));
        return container;
    }

    /**
     * 暴露 RedisTemplate 别名，避免每处都写泛型。
     */
    @Bean(name = "wsBusRedisTemplate")
    public RedisTemplate<String, Object> wsBusRedisTemplate(RedisConnectionFactory factory,
            Jackson2JsonRedisSerializer<PushMessage> serializer)
    {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setDefaultSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }
}
