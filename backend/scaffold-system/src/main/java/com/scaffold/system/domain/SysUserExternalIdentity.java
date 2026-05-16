package com.scaffold.system.domain;

import java.util.Date;

/**
 * 外部身份绑定 sys_user_external_identity（与 OAuth2 / OIDC SSO 协同）。
 *
 * @author scaffold
 */
public class SysUserExternalIdentity
{
    private Long id;
    /** 本地 sys_user.user_id */
    private Long userId;
    /** IDP 注册名（与 spring.security.oauth2.client.registration.&lt;id&gt; 对齐） */
    private String provider;
    /** IDP 返回的稳定 ID（OIDC 中是 {@code sub}） */
    private String subject;
    /** 首次绑定时的邮箱，仅作展示 */
    private String email;
    /** 首次绑定时的 IDP 原始 claims（JSON 字符串） */
    private String rawProfile;
    private Date boundAt;
    private Date lastLoginAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRawProfile() { return rawProfile; }
    public void setRawProfile(String rawProfile) { this.rawProfile = rawProfile; }
    public Date getBoundAt() { return boundAt; }
    public void setBoundAt(Date boundAt) { this.boundAt = boundAt; }
    public Date getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Date lastLoginAt) { this.lastLoginAt = lastLoginAt; }
}
