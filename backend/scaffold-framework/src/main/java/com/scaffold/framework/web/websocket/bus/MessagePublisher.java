package com.scaffold.framework.web.websocket.bus;

/**
 * 推送总线门面：业务侧只需注入此接口，不感知底层是单机 STOMP 还是 Redis Pub/Sub fan-out。
 *
 * @author scaffold
 */
public interface MessagePublisher
{
    /**
     * 点对点推送：投递到 {@code /user/<username>/queue/notice}。
     */
    void toUser(String username, String type, Object payload);

    void toUser(String username, String type, String messageId, Object payload);

    /**
     * 主题广播：投递到 {@code /topic/<topic>}。
     */
    void toTopic(String topic, String type, Object payload);

    void toTopic(String topic, String type, String messageId, Object payload);
}
