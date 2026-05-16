package com.scaffold.framework.web.websocket;

import java.security.Principal;
import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import com.scaffold.common.core.domain.model.LoginUser;

/**
 * 给 STOMP 会话绑定 {@link StompPrincipal}，使 {@code /user/queue/...} 能按用户精确路由。
 *
 * @author scaffold
 */
public class AuthHandshakeHandler extends DefaultHandshakeHandler
{
    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
            Map<String, Object> attributes)
    {
        Object loginUser = attributes.get(JwtHandshakeInterceptor.ATTR_LOGIN_USER);
        if (loginUser instanceof LoginUser user)
        {
            Long userId = JwtHandshakeInterceptor.extractUserId(user);
            return new StompPrincipal(user.getUsername(), userId);
        }
        return null;
    }
}
