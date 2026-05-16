package com.scaffold.framework.web.websocket.bus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class RedisMessageBusTest
{
    private RedisTemplate<String, Object> redisTemplate;
    private SimpMessagingTemplate stomp;
    private RedisMessageBus bus;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @BeforeEach
    void setUp()
    {
        redisTemplate = mock(RedisTemplate.class);
        stomp = mock(SimpMessagingTemplate.class);
        ObjectProvider<MessageBusRecorder> recorderProvider = mock(ObjectProvider.class);
        bus = new RedisMessageBus((RedisTemplate) redisTemplate, stomp, recorderProvider, "scaffold:ws:bus");
    }

    @Test
    void toUserPublishesPushMessageToConfiguredChannel()
    {
        bus.toUser("alice", "notice", "{\"hello\":\"world\"}");
        verify(redisTemplate, times(1)).convertAndSend(eq("scaffold:ws:bus"), any(PushMessage.class));
        verify(stomp, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void toTopicPublishesPushMessageToConfiguredChannel()
    {
        bus.toTopic("global", "broadcast", "hello");
        verify(redisTemplate, times(1)).convertAndSend(eq("scaffold:ws:bus"), any(PushMessage.class));
    }

    @Test
    void deliverLocalRoutesUserScopeToUserDestination()
    {
        PushMessage msg = PushMessage.toUser("bob", "notice", "id-1", "hi");
        bus.deliverLocal(msg);
        verify(stomp).convertAndSendToUser("bob", "/queue/notice", msg);
    }

    @Test
    void deliverLocalRoutesTopicScopeToTopicDestination()
    {
        PushMessage msg = PushMessage.toTopic("ops", "alert", "id-2", "fire");
        bus.deliverLocal(msg);
        verify(stomp).convertAndSend("/topic/ops", msg);
    }

    @Test
    void publishFallsBackToLocalDeliveryOnRedisFailure()
    {
        doThrow(new RuntimeException("redis down")).when(redisTemplate).convertAndSend(anyString(), any());
        bus.toUser("carol", "notice", "id-3", "payload");
        verify(stomp).convertAndSendToUser(eq("carol"), eq("/queue/notice"), any(PushMessage.class));
    }

    @Test
    void pushMessageFactoryHelpersAttachTimestampAndScope()
    {
        PushMessage user = PushMessage.toUser("u", "t", "i", "p");
        assertThat(user.getScope()).isEqualTo(PushMessage.Scope.USER);
        assertThat(user.getTimestamp()).isPositive();

        PushMessage topic = PushMessage.toTopic("t", "y", "j", "p");
        assertThat(topic.getScope()).isEqualTo(PushMessage.Scope.TOPIC);
        assertThat(topic.getTarget()).isEqualTo("t");
    }
}
