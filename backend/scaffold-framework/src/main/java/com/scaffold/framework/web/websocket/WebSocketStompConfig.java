package com.scaffold.framework.web.websocket;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import com.scaffold.framework.web.service.TokenService;

/**
 * 启用 WebSocket + STOMP，挂载默认端点 {@code /ws}，对所有进入握手做 JWT 鉴权。
 * <p>
 * 客户端可订阅：
 * <ul>
 *   <li>{@code /user/queue/notice} - 单用户消息（点对点）</li>
 *   <li>{@code /topic/<channel>} - 主题广播（多人收）</li>
 * </ul>
 * 多实例部署时由 {@link com.scaffold.framework.web.websocket.bus.RedisMessageBus}
 * 通过 Redis Pub/Sub fan-out，确保消息能投递到所有持有目标用户连接的节点。
 *
 * @author scaffold
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketStompConfig implements WebSocketMessageBrokerConfigurer
{
    private final TokenService tokenService;

    @Value("${websocket.allowed-origins:*}")
    private String allowedOrigins;

    public WebSocketStompConfig(TokenService tokenService)
    {
        this.tokenService = tokenService;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry)
    {
        registry.enableSimpleBroker("/queue", "/topic");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry)
    {
        List<String> origins = parseOrigins(allowedOrigins);
        registry.addEndpoint("/ws")
                .addInterceptors(new JwtHandshakeInterceptor(tokenService))
                .setHandshakeHandler(new AuthHandshakeHandler())
                .setAllowedOriginPatterns(origins.toArray(new String[0]));
    }

    private List<String> parseOrigins(String raw)
    {
        if (raw == null || raw.isBlank()) return List.of("*");
        return Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
