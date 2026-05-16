package com.scaffold.framework.web.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.common.core.domain.entity.SysUser;
import com.scaffold.common.core.domain.model.LoginUser;
import com.scaffold.common.exception.BizException;
import com.scaffold.framework.web.service.UserDetailsServiceImpl;
import com.scaffold.system.domain.SysUserExternalIdentity;
import com.scaffold.system.mapper.SysUserExternalIdentityMapper;
import com.scaffold.system.service.ISysUserService;

class OAuth2UserResolverTest
{
    private ISysUserService userService;
    private SysUserExternalIdentityMapper identityMapper;
    private UserDetailsServiceImpl userDetailsService;
    private OAuth2UserResolver resolver;

    @BeforeEach
    void setUp()
    {
        userService = mock(ISysUserService.class);
        identityMapper = mock(SysUserExternalIdentityMapper.class);
        userDetailsService = mock(UserDetailsServiceImpl.class);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        ObjectMapper objectMapper = new ObjectMapper();
        resolver = new OAuth2UserResolver(userService, identityMapper, userDetailsService, encoder, objectMapper);
        ReflectionTestUtils.setField(resolver, "autoProvision", true);
        ReflectionTestUtils.setField(resolver, "autoProvisionStatus", "1");
        // userDetailsService.createLoginUser 在生产代码里需要权限，这里返回 mock 即可
        LoginUser stub = new LoginUser();
        lenient().when(userDetailsService.createLoginUser(any(SysUser.class))).thenReturn(stub);
    }

    private OAuth2User principal(Map<String, Object> attrs)
    {
        Map<String, Object> attributes = new HashMap<>(attrs);
        attributes.putIfAbsent("sub", "sub-1");
        return new DefaultOAuth2User(java.util.Collections.emptyList(), attributes, "sub");
    }

    @Test
    void resolveExistingBindingUpdatesLastLogin()
    {
        SysUserExternalIdentity bound = new SysUserExternalIdentity();
        bound.setId(7L);
        bound.setUserId(99L);
        when(identityMapper.selectByProviderAndSubject("azure", "sub-1")).thenReturn(bound);
        SysUser user = new SysUser();
        user.setUserId(99L);
        when(userService.selectUserById(99L)).thenReturn(user);

        Object result = resolver.resolveOrProvision("azure", principal(Map.of("email", "a@b.com")));
        assertThat(result).isNotNull();
        verify(identityMapper).updateLastLoginAt(7L);
        verify(identityMapper, never()).insert(any());
        verify(userService, never()).insertUser(any());
    }

    @Test
    void resolveByEmailBindsExistingUser()
    {
        when(identityMapper.selectByProviderAndSubject(eq("azure"), anyString())).thenReturn(null);
        SysUser existing = new SysUser();
        existing.setUserId(101L);
        when(userService.findByEmail("a@b.com")).thenReturn(existing);

        resolver.resolveOrProvision("azure", principal(Map.of("email", "a@b.com")));
        verify(identityMapper, times(1)).insert(any());
        verify(userService, never()).insertUser(any());
    }

    @Test
    void provisionNewUserWhenNoMatchAndAutoProvisionOn()
    {
        when(identityMapper.selectByProviderAndSubject(anyString(), anyString())).thenReturn(null);
        when(userService.findByEmail(anyString())).thenReturn(null);
        // userName 不冲突
        when(userService.selectUserByUserName(anyString())).thenReturn(null);
        // insertUser 时给入参填 userId
        when(userService.insertUser(any())).thenAnswer(inv -> {
            SysUser u = inv.getArgument(0);
            u.setUserId(123L);
            return 1;
        });

        resolver.resolveOrProvision("azure",
                principal(Map.of("email", "new@b.com", "preferred_username", "new.user")));

        verify(userService, times(1)).insertUser(any());
        verify(identityMapper, times(1)).insert(any());
    }

    @Test
    void rejectsWhenAutoProvisionOff()
    {
        ReflectionTestUtils.setField(resolver, "autoProvision", false);
        when(identityMapper.selectByProviderAndSubject(anyString(), anyString())).thenReturn(null);
        when(userService.findByEmail(anyString())).thenReturn(null);

        assertThatThrownBy(() -> resolver.resolveOrProvision("azure",
                principal(Map.of("email", "blocked@b.com"))))
                .isInstanceOf(BizException.class);
    }

    @Test
    void boundIdentityWithDeletedUserThrows()
    {
        SysUserExternalIdentity bound = new SysUserExternalIdentity();
        bound.setId(2L);
        bound.setUserId(404L);
        when(identityMapper.selectByProviderAndSubject(anyString(), anyString())).thenReturn(bound);
        when(userService.selectUserById(anyLong())).thenReturn(null);

        assertThatThrownBy(() -> resolver.resolveOrProvision("azure", principal(Map.of())))
                .isInstanceOf(BizException.class);
    }
}
