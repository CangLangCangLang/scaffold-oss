package com.scaffold.framework.web.websocket;

import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import com.scaffold.common.core.domain.entity.SysUser;
import com.scaffold.common.core.domain.model.LoginUser;
import com.scaffold.framework.web.service.TokenService;

/**
 * WebSocket 握手鉴权拦截器：
 * <ul>
 *   <li>从查询参数 {@code ?token=xxx} 或请求头 {@code Authorization} 中提取 JWT</li>
 *   <li>调用 {@link TokenService#getLoginUser(String)} 解析当前用户</li>
 *   <li>解析成功则把 {@link LoginUser} 写入 attributes，供后续 {@link AuthHandshakeHandler} 取用</li>
 * </ul>
 *
 * @author scaffold
 */
public class JwtHandshakeInterceptor implements HandshakeInterceptor
{
    private static final Logger log = LoggerFactory.getLogger(JwtHandshakeInterceptor.class);

    public static final String ATTR_LOGIN_USER = "scaffold:loginUser";

    private final TokenService tokenService;

    public JwtHandshakeInterceptor(TokenService tokenService)
    {
        this.tokenService = tokenService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Map<String, Object> attributes)
    {
        String token = extractToken(request);
        if (token == null || token.isBlank())
        {
            log.debug("WebSocket 握手缺少 token，拒绝连接 uri={}", request.getURI());
            return false;
        }
        LoginUser loginUser = tokenService.getLoginUser(token);
        if (loginUser == null)
        {
            log.debug("WebSocket 握手 token 无效或已过期 uri={}", request.getURI());
            return false;
        }
        attributes.put(ATTR_LOGIN_USER, loginUser);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Exception exception)
    {
        // no-op
    }

    private String extractToken(ServerHttpRequest request)
    {
        if (request instanceof ServletServerHttpRequest servletRequest)
        {
            HttpServletRequest http = servletRequest.getServletRequest();
            String token = http.getParameter("token");
            if (token != null && !token.isBlank()) return token;
            String header = http.getHeader("Authorization");
            if (header != null && !header.isBlank()) return header;
        }
        return null;
    }

    public static Long extractUserId(LoginUser loginUser)
    {
        if (loginUser == null) return null;
        SysUser user = loginUser.getUser();
        return user == null ? null : user.getUserId();
    }
}
