package com.scaffold.web.controller.system;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.scaffold.common.core.domain.AjaxResult;

/**
 * SSO 元数据：列出当前可用的第三方登录入口，供登录页渲染按钮。
 * <p>
 * - 不需要登录即可访问（在 SecurityConfig 已放行 /sso/**）。
 * - 当配置中心未注册任何 IDP 时，返回空数组，前端自动隐藏入口。
 *
 * @author scaffold
 */
@RestController
@RequestMapping("/sso")
public class SsoMetaController
{
    @Autowired(required = false)
    private ClientRegistrationRepository clientRegistrationRepository;

    /**
     * 自定义按钮显示名 / 图标，按 IDP 注册名（key）配置。
     * 例：sso.providers.azure.label=Azure AD, sso.providers.azure.icon=mdi-microsoft
     */
    @Value("#{${sso.providers:{}}}")
    private java.util.Map<String, String> providersMeta;

    @GetMapping("/providers")
    public AjaxResult listProviders()
    {
        if (clientRegistrationRepository == null)
        {
            return AjaxResult.success(Collections.emptyList());
        }
        List<java.util.Map<String, Object>> result = new ArrayList<>();
        if (clientRegistrationRepository instanceof InMemoryClientRegistrationRepository repo)
        {
            for (ClientRegistration registration : repo)
            {
                java.util.Map<String, Object> item = new java.util.HashMap<>();
                item.put("id", registration.getRegistrationId());
                item.put("label", lookupMeta(registration.getRegistrationId(), "label",
                        registration.getClientName() != null ? registration.getClientName() : registration.getRegistrationId()));
                item.put("icon", lookupMeta(registration.getRegistrationId(), "icon", null));
                item.put("authorizationUri", "/oauth2/authorization/" + registration.getRegistrationId());
                result.add(item);
            }
        }
        return AjaxResult.success(result);
    }

    private String lookupMeta(String registrationId, String suffix, String fallback)
    {
        if (providersMeta == null) return fallback;
        return providersMeta.getOrDefault(registrationId + "." + suffix, fallback);
    }
}
