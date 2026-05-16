package com.scaffold.module.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import com.scaffold.common.core.domain.entity.SysUser;
import com.scaffold.common.core.domain.model.LoginUser;
import com.scaffold.common.exception.ServiceException;
import com.scaffold.common.utils.SecurityUtils;
import com.scaffold.module.file.domain.SysFile;
import com.scaffold.module.file.domain.SysFileShare;
import com.scaffold.module.file.dto.ShareCreateRequest;
import com.scaffold.module.file.mapper.SysFileMapper;
import com.scaffold.module.file.mapper.SysFileShareMapper;

/**
 * ShareService 单测。覆盖：token 生成、过期、一次性、密码、状态转换。
 */
class ShareServiceTest
{
    private SysFileShareMapper shareMapper;
    private SysFileMapper fileMapper;
    private ShareService shareService;

    @BeforeEach
    void setUp()
    {
        shareMapper = mock(SysFileShareMapper.class);
        fileMapper = mock(SysFileMapper.class);
        shareService = new ShareService();
        ReflectionTestUtils.setField(shareService, "shareMapper", shareMapper);
        ReflectionTestUtils.setField(shareService, "fileMapper", fileMapper);

        SysUser sys = new SysUser();
        sys.setUserId(7L);
        sys.setUserName("alice");
        LoginUser u = new LoginUser(7L, 1L, sys, null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(u, null, Collections.emptyList()));
    }

    @AfterEach
    void tearDown() { SecurityContextHolder.clearContext(); }

    @Test
    void createGeneratesTokenAndDefaults()
    {
        SysFile target = new SysFile();
        target.setId(10L);
        target.setDelFlag("0");
        when(fileMapper.selectById(10L)).thenReturn(target);

        ShareCreateRequest req = new ShareCreateRequest();
        req.setFileId(10L);
        req.setExpireDays(7);
        req.setOneTime("0");

        SysFileShare s = shareService.create(req);
        assertThat(s.getToken()).isNotBlank().hasSize(22);
        assertThat(s.getExpireAt()).isNotNull();
        assertThat(s.getOneTime()).isEqualTo("0");
        assertThat(s.getStatus()).isEqualTo("0");
        assertThat(s.getVisits()).isZero();
        assertThat(s.getCreateBy()).isEqualTo("alice");
        verify(shareMapper).insert(s);
    }

    @Test
    void createBcryptsPasswordWhenProvided()
    {
        SysFile target = new SysFile();
        target.setId(10L);
        target.setDelFlag("0");
        when(fileMapper.selectById(10L)).thenReturn(target);

        ShareCreateRequest req = new ShareCreateRequest();
        req.setFileId(10L);
        req.setPassword("hello");

        SysFileShare s = shareService.create(req);
        assertThat(s.getPasswordHash()).isNotBlank().doesNotContain("hello");
        assertThat(SecurityUtils.matchesPassword("hello", s.getPasswordHash())).isTrue();
    }

    @Test
    void createRejectsMissingFileOrSoftDeleted()
    {
        when(fileMapper.selectById(99L)).thenReturn(null);
        ShareCreateRequest req = new ShareCreateRequest();
        req.setFileId(99L);
        assertThatThrownBy(() -> shareService.create(req))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("不存在");

        SysFile target = new SysFile();
        target.setId(99L);
        target.setDelFlag("2");
        when(fileMapper.selectById(99L)).thenReturn(target);
        assertThatThrownBy(() -> shareService.create(req))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("已删除");
    }

    @Test
    void accessRejectsExpired()
    {
        SysFileShare s = new SysFileShare();
        s.setId(1L);
        s.setStatus("0");
        s.setExpireAt(new Date(System.currentTimeMillis() - 1000));
        s.setOneTime("0");
        s.setVisits(0);
        when(shareMapper.selectByToken(eq("t"))).thenReturn(s);
        assertThatThrownBy(() -> shareService.access("t", null))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("过期");
    }

    @Test
    void accessRejectsDisabled()
    {
        SysFileShare s = new SysFileShare();
        s.setStatus("1");
        when(shareMapper.selectByToken(eq("t"))).thenReturn(s);
        assertThatThrownBy(() -> shareService.access("t", null))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("停用");
    }

    @Test
    void accessOneTimeFlipsStatusToConsumed()
    {
        SysFile target = new SysFile();
        target.setId(10L);
        target.setDelFlag("0");
        SysFileShare s = new SysFileShare();
        s.setId(1L);
        s.setStatus("0");
        s.setOneTime("1");
        s.setVisits(0);
        s.setFileId(10L);
        when(shareMapper.selectByToken(eq("t"))).thenReturn(s);
        when(fileMapper.selectById(10L)).thenReturn(target);

        SysFile got = shareService.access("t", null);
        assertThat(got.getId()).isEqualTo(10L);
        verify(shareMapper).incrVisits(1L);
        verify(shareMapper).updateStatus(1L, "2");
    }

    @Test
    void accessRejectsConsumedOneTime()
    {
        SysFileShare s = new SysFileShare();
        s.setId(1L);
        s.setStatus("0");
        s.setOneTime("1");
        s.setVisits(1);
        when(shareMapper.selectByToken(eq("t"))).thenReturn(s);
        assertThatThrownBy(() -> shareService.access("t", null))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("已使用");
    }

    @Test
    void accessRejectsWrongPassword()
    {
        SysFileShare s = new SysFileShare();
        s.setId(1L);
        s.setStatus("0");
        s.setOneTime("0");
        s.setVisits(0);
        s.setPasswordHash(SecurityUtils.encryptPassword("right"));
        when(shareMapper.selectByToken(eq("t"))).thenReturn(s);
        assertThatThrownBy(() -> shareService.access("t", "wrong"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("密码");
        // 不应 incrVisits
        verify(shareMapper, never()).incrVisits(anyLong());
    }

    @Test
    void accessAcceptsCorrectPassword()
    {
        SysFile target = new SysFile();
        target.setId(10L);
        target.setDelFlag("0");
        SysFileShare s = new SysFileShare();
        s.setId(1L);
        s.setStatus("0");
        s.setOneTime("0");
        s.setVisits(0);
        s.setFileId(10L);
        s.setPasswordHash(SecurityUtils.encryptPassword("right"));
        when(shareMapper.selectByToken(eq("t"))).thenReturn(s);
        when(fileMapper.selectById(10L)).thenReturn(target);

        SysFile got = shareService.access("t", "right");
        assertThat(got.getId()).isEqualTo(10L);
        verify(shareMapper).incrVisits(1L);
    }

    @Test
    void disableAndRemoveDelegate()
    {
        when(shareMapper.updateStatus(eq(1L), eq("1"))).thenReturn(1);
        when(shareMapper.deleteById(eq(1L))).thenReturn(1);

        assertThat(shareService.disable(1L)).isEqualTo(1);
        assertThat(shareService.remove(1L)).isEqualTo(1);
    }
}
