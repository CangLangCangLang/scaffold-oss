package com.scaffold.framework.web.oauth2;

import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.common.core.domain.entity.SysUser;
import com.scaffold.common.core.domain.model.LoginUser;
import com.scaffold.common.exception.BizException;
import com.scaffold.common.constant.BizCode;
import com.scaffold.common.utils.StringUtils;
import com.scaffold.framework.web.service.UserDetailsServiceImpl;
import com.scaffold.system.domain.SysUserExternalIdentity;
import com.scaffold.system.mapper.SysUserExternalIdentityMapper;
import com.scaffold.system.service.ISysUserService;

/**
 * 第三方登录后 → 本地用户的解析器。
 * <p>
 * 解析顺序：
 * <ol>
 *   <li>按 {@code (provider, subject)} 在 {@code sys_user_external_identity} 中找已有绑定</li>
 *   <li>未绑定 → 取 IDP 返回的 email，回查本地 {@code sys_user.email}；命中则做"补绑"</li>
 *   <li>仍未命中 → 按 {@code sso.auto-provision} 自动开户（只生成基础账号，不分配角色）</li>
 * </ol>
 * 自动开户产生的账号默认 disabled，需要管理员审核启用，避免被恶意 IDP 灌入。
 *
 * @author scaffold
 */
@Service
public class OAuth2UserResolver
{
    private static final Logger log = LoggerFactory.getLogger(OAuth2UserResolver.class);

    private final ISysUserService userService;
    private final SysUserExternalIdentityMapper identityMapper;
    private final UserDetailsServiceImpl userDetailsService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Value("${sso.auto-provision:true}")
    private boolean autoProvision;

    @Value("${sso.auto-provision-status:1}")
    private String autoProvisionStatus;

    public OAuth2UserResolver(ISysUserService userService,
            SysUserExternalIdentityMapper identityMapper,
            UserDetailsServiceImpl userDetailsService,
            BCryptPasswordEncoder passwordEncoder,
            ObjectMapper objectMapper)
    {
        this.userService = userService;
        this.identityMapper = identityMapper;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    /**
     * @param registrationId IDP 注册名（与 spring.security.oauth2.client.registration.&lt;id&gt; 一致）
     * @param principal      Spring Security 解析出来的 OAuth2 / OIDC 用户
     * @return 已加载权限信息的 {@link LoginUser}
     */
    @Transactional
    public LoginUser resolveOrProvision(String registrationId, OAuth2User principal)
    {
        String subject = pickSubject(principal);
        if (StringUtils.isEmpty(subject))
        {
            throw new BizException(BizCode.UNAUTHORIZED, "IDP 返回的用户标识为空");
        }
        String email = strAttr(principal, "email");
        String preferredName = firstNonEmpty(
                strAttr(principal, "preferred_username"),
                strAttr(principal, "username"),
                strAttr(principal, "login"),
                strAttr(principal, "name"));
        String displayName = firstNonEmpty(
                strAttr(principal, "name"),
                strAttr(principal, "nickname"),
                preferredName);

        SysUserExternalIdentity bound = identityMapper.selectByProviderAndSubject(registrationId, subject);
        SysUser sysUser;
        if (bound != null)
        {
            sysUser = userService.selectUserById(bound.getUserId());
            if (sysUser == null)
            {
                throw new BizException(BizCode.UNAUTHORIZED,
                        "外部账号关联的本地用户已被删除，请联系管理员");
            }
            identityMapper.updateLastLoginAt(bound.getId());
        }
        else
        {
            sysUser = StringUtils.isNotEmpty(email)
                    ? findByEmailIfPresent(email)
                    : null;
            if (sysUser == null)
            {
                if (!autoProvision)
                {
                    throw new BizException(BizCode.FORBIDDEN,
                            "未在系统找到对应账号，且未开启 SSO 自动开户");
                }
                sysUser = provisionUser(registrationId, subject, email, preferredName, displayName);
            }
            insertBinding(registrationId, subject, sysUser, email, principal);
        }

        return (LoginUser) userDetailsService.createLoginUser(sysUser);
    }

    private SysUser findByEmailIfPresent(String email)
    {
        return userService.findByEmail(email);
    }

    private SysUser provisionUser(String registrationId, String subject, String email,
            String preferredName, String displayName)
    {
        SysUser user = new SysUser();
        String userName = sanitizeUserName(preferredName, registrationId, subject);
        user.setUserName(userName);
        user.setNickName(StringUtils.isNotEmpty(displayName) ? displayName : userName);
        user.setEmail(email);
        // 自动开户用户给一个高强度随机密码，禁止本地密码登录（除非管理员重置）
        user.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
        user.setStatus(autoProvisionStatus); // 默认 1=禁用，需管理员启用
        user.setCreateBy("sso:" + registrationId);
        user.setCreateTime(new Date());
        userService.insertUser(user);
        if (user.getUserId() == null)
        {
            throw new BizException(BizCode.INTERNAL_ERROR, "SSO 自动开户失败：未生成 userId");
        }
        log.info("SSO 自动开户 provider={} subject={} userId={} status={}",
                registrationId, subject, user.getUserId(), user.getStatus());
        return user;
    }

    private void insertBinding(String registrationId, String subject, SysUser sysUser,
            String email, OAuth2User principal)
    {
        SysUserExternalIdentity identity = new SysUserExternalIdentity();
        identity.setUserId(sysUser.getUserId());
        identity.setProvider(registrationId);
        identity.setSubject(subject);
        identity.setEmail(email);
        identity.setBoundAt(new Date());
        identity.setLastLoginAt(new Date());
        try
        {
            identity.setRawProfile(objectMapper.writeValueAsString(principal.getAttributes()));
        }
        catch (JsonProcessingException ignored)
        {
            // 原始 claims 序列化失败不影响绑定
        }
        identityMapper.insert(identity);
    }

    private static String pickSubject(OAuth2User principal)
    {
        // OIDC 标准是 sub；非 OIDC 退化为 name（即 OAuth2User 的主键属性）
        String sub = strAttr(principal, "sub");
        return StringUtils.isNotEmpty(sub) ? sub : principal.getName();
    }

    private static String firstNonEmpty(String... values)
    {
        if (values == null) return null;
        for (String v : values)
        {
            if (StringUtils.isNotEmpty(v)) return v;
        }
        return null;
    }

    private static String strAttr(OAuth2User principal, String key)
    {
        Object value = principal.getAttributes() == null ? null : principal.getAttributes().get(key);
        return value == null ? null : value.toString();
    }

    /**
     * 把 IDP 返回的用户名洗成符合本地约束（≥ 2 位、非数字开头）。
     * 冲突时附加 provider 后缀。
     */
    private String sanitizeUserName(String preferredName, String registrationId, String subject)
    {
        String base = StringUtils.isNotEmpty(preferredName)
                ? preferredName.trim().replaceAll("[^A-Za-z0-9_\\-.]", "")
                : registrationId + "_" + subject;
        if (base.length() < 2) base = registrationId + "_" + subject;
        if (Character.isDigit(base.charAt(0))) base = "u_" + base;
        String candidate = base;
        int suffix = 1;
        while (userService.selectUserByUserName(candidate) != null)
        {
            suffix++;
            candidate = base + "_" + suffix;
        }
        return candidate;
    }
}
