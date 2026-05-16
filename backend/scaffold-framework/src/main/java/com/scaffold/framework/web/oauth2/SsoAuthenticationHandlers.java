package com.scaffold.framework.web.oauth2;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import com.scaffold.common.core.domain.model.LoginUser;
import com.scaffold.framework.web.service.TokenService;

/**
 * OAuth2 Authorization Code 登录成功 / 失败处理。
 * <p>
 * 成功：解析 / 自动开户后由 {@link TokenService} 生成本地 JWT，再 302 到前端 SSO 回调页。
 * 失败：把错误信息编码到 query 参数，便于前端展示。
 *
 * @author scaffold
 */
@Component
public class SsoAuthenticationHandlers implements AuthenticationSuccessHandler, AuthenticationFailureHandler
{
    private static final Logger log = LoggerFactory.getLogger(SsoAuthenticationHandlers.class);

    private final OAuth2UserResolver userResolver;
    private final TokenService tokenService;
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Value("${sso.front-callback:/sso/callback}")
    private String frontCallback;

    @Value("${sso.failure-redirect:/login}")
    private String failureRedirect;

    public SsoAuthenticationHandlers(OAuth2UserResolver userResolver, TokenService tokenService)
    {
        this.userResolver = userResolver;
        this.tokenService = tokenService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException
    {
        if (!(authentication instanceof OAuth2AuthenticationToken oauth))
        {
            throw new ServletException("Expected OAuth2AuthenticationToken, got "
                    + (authentication == null ? "null" : authentication.getClass().getName()));
        }
        String registrationId = oauth.getAuthorizedClientRegistrationId();
        LoginUser loginUser;
        try
        {
            loginUser = userResolver.resolveOrProvision(registrationId, oauth.getPrincipal());
        }
        catch (Exception ex)
        {
            log.warn("SSO 解析失败 provider={}", registrationId, ex);
            redirectToFailure(request, response, ex.getMessage());
            return;
        }
        String token = tokenService.createToken(loginUser);
        String target = frontCallback + (frontCallback.contains("?") ? "&" : "?")
                + "token=" + URLEncoder.encode(token, StandardCharsets.UTF_8)
                + "&provider=" + URLEncoder.encode(registrationId, StandardCharsets.UTF_8);
        redirectStrategy.sendRedirect(request, response, target);
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException
    {
        log.warn("SSO 登录失败：{}", exception.getMessage());
        redirectToFailure(request, response, exception.getMessage());
    }

    private void redirectToFailure(HttpServletRequest request, HttpServletResponse response, String reason)
            throws IOException
    {
        String target = failureRedirect + (failureRedirect.contains("?") ? "&" : "?")
                + "ssoError=" + URLEncoder.encode(reason == null ? "sso_failed" : reason, StandardCharsets.UTF_8);
        redirectStrategy.sendRedirect(request, response, target);
    }
}
