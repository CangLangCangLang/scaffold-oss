package com.scaffold.framework.web.websocket;

import java.security.Principal;

/**
 * WebSocket 会话上的用户主体，绑定登录用户名，方便 STOMP user destination 路由。
 *
 * @author scaffold
 */
public final class StompPrincipal implements Principal
{
    private final String name;
    private final Long userId;

    public StompPrincipal(String name, Long userId)
    {
        this.name = name;
        this.userId = userId;
    }

    @Override
    public String getName()
    {
        return name;
    }

    public Long getUserId()
    {
        return userId;
    }
}
