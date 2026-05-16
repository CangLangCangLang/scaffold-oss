package com.scaffold.module.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import com.scaffold.common.core.domain.entity.SysUser;
import com.scaffold.common.core.domain.model.LoginUser;
import com.scaffold.module.file.mapper.SysFileMapper;
import com.scaffold.module.file.mapper.SysFileRefMapper;

/**
 * FileRefService 单测 — 验证 attach / detach 与 ref_count 同步原子性。
 */
class FileRefServiceTest
{
    private SysFileRefMapper refMapper;
    private SysFileMapper fileMapper;
    private FileRefService refService;

    @BeforeEach
    void setUp()
    {
        refMapper = mock(SysFileRefMapper.class);
        fileMapper = mock(SysFileMapper.class);
        refService = new FileRefService();
        ReflectionTestUtils.setField(refService, "refMapper", refMapper);
        ReflectionTestUtils.setField(refService, "fileMapper", fileMapper);

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
    void attachIncrementsCountWhenInsertSucceeds()
    {
        when(refMapper.insertIgnore(any())).thenReturn(1);
        boolean ok = refService.attach(10L, "cms", "article", "42");
        assertThat(ok).isTrue();
        verify(fileMapper).incrRefCount(10L);
    }

    @Test
    void attachSkipsIncrementWhenIgnoredAsDup()
    {
        when(refMapper.insertIgnore(any())).thenReturn(0); // 已有同 (file, module, type, id)
        boolean ok = refService.attach(10L, "cms", "article", "42");
        assertThat(ok).isFalse();
        verify(fileMapper, never()).incrRefCount(anyLong());
    }

    @Test
    void detachDecrementsCountWhenRowFound()
    {
        when(refMapper.deleteOne(eq(10L), eq("cms"), eq("article"), eq("42"))).thenReturn(1);
        boolean ok = refService.detach(10L, "cms", "article", "42");
        assertThat(ok).isTrue();
        verify(fileMapper).decrRefCount(10L);
    }

    @Test
    void detachSkipsDecrementWhenNothingDeleted()
    {
        when(refMapper.deleteOne(anyLong(), any(), any(), any())).thenReturn(0);
        boolean ok = refService.detach(10L, "cms", "article", "42");
        assertThat(ok).isFalse();
        verify(fileMapper, never()).decrRefCount(anyLong());
    }
}
