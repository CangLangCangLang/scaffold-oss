package com.scaffold.module.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
import com.scaffold.common.exception.ServiceException;
import com.scaffold.module.file.domain.SysFileFolder;
import com.scaffold.module.file.dto.FolderRequest;
import com.scaffold.module.file.mapper.SysFileFolderMapper;

/**
 * FolderService 单测。覆盖：path 拼接、唯一约束、跨用户保护、软删递归。
 */
class FolderServiceTest
{
    private SysFileFolderMapper folderMapper;
    private FolderService folderService;

    @BeforeEach
    void setUp()
    {
        folderMapper = mock(SysFileFolderMapper.class);
        folderService = new FolderService();
        ReflectionTestUtils.setField(folderService, "folderMapper", folderMapper);

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
    void createAtRootBuildsLeadingSlashPath()
    {
        when(folderMapper.selectByOwnerAndPath(eq("alice"), anyString())).thenReturn(null);

        FolderRequest req = new FolderRequest();
        req.setName("Q3 报表");
        req.setParentId(0L);

        SysFileFolder f = folderService.create(req);
        assertThat(f.getPath()).isEqualTo("/Q3 报表");
        assertThat(f.getOwner()).isEqualTo("alice");
        assertThat(f.getParentId()).isZero();
        verify(folderMapper).insert(f);
    }

    @Test
    void createUnderParentChainsPath()
    {
        SysFileFolder parent = new SysFileFolder();
        parent.setId(10L);
        parent.setOwner("alice");
        parent.setPath("/research");
        when(folderMapper.selectById(10L)).thenReturn(parent);
        when(folderMapper.selectByOwnerAndPath(anyString(), anyString())).thenReturn(null);

        FolderRequest req = new FolderRequest();
        req.setName("papers");
        req.setParentId(10L);

        SysFileFolder f = folderService.create(req);
        assertThat(f.getPath()).isEqualTo("/research/papers");
        assertThat(f.getParentId()).isEqualTo(10L);
    }

    @Test
    void createRejectsCrossOwnerParent()
    {
        SysFileFolder parent = new SysFileFolder();
        parent.setId(10L);
        parent.setOwner("bob");
        parent.setPath("/bobspace");
        when(folderMapper.selectById(10L)).thenReturn(parent);

        FolderRequest req = new FolderRequest();
        req.setName("papers");
        req.setParentId(10L);

        assertThatThrownBy(() -> folderService.create(req))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("不属于");
    }

    @Test
    void createRejectsDuplicatePath()
    {
        SysFileFolder dup = new SysFileFolder();
        dup.setId(99L);
        when(folderMapper.selectByOwnerAndPath(eq("alice"), eq("/Q3 报表"))).thenReturn(dup);

        FolderRequest req = new FolderRequest();
        req.setName("Q3 报表");
        req.setParentId(0L);
        assertThatThrownBy(() -> folderService.create(req))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("已存在");
    }

    @Test
    void renameRejectsCrossOwner()
    {
        SysFileFolder cur = new SysFileFolder();
        cur.setId(10L);
        cur.setOwner("bob");
        cur.setPath("/foo");
        cur.setParentId(0L);
        when(folderMapper.selectById(10L)).thenReturn(cur);

        FolderRequest req = new FolderRequest();
        req.setId(10L);
        req.setName("bar");
        assertThatThrownBy(() -> folderService.rename(req))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("没有权限");
    }

    @Test
    void renameUpdatesPath()
    {
        SysFileFolder cur = new SysFileFolder();
        cur.setId(10L);
        cur.setOwner("alice");
        cur.setPath("/old");
        cur.setParentId(0L);
        when(folderMapper.selectById(10L)).thenReturn(cur);
        when(folderMapper.selectByOwnerAndPath(eq("alice"), eq("/new"))).thenReturn(null);
        when(folderMapper.updateById(any())).thenReturn(1);

        FolderRequest req = new FolderRequest();
        req.setId(10L);
        req.setName("new");

        assertThat(folderService.rename(req)).isEqualTo(1);
        verify(folderMapper).updateById(any(SysFileFolder.class));
    }

    @Test
    void removeSoftDeletesEntireSubtree()
    {
        SysFileFolder cur = new SysFileFolder();
        cur.setId(10L);
        cur.setOwner("alice");
        cur.setPath("/research");
        when(folderMapper.selectById(10L)).thenReturn(cur);
        when(folderMapper.softDeleteSubtree(eq("alice"), eq("/research"))).thenReturn(3);

        assertThat(folderService.remove(10L)).isEqualTo(3);
        verify(folderMapper).softDeleteSubtree("alice", "/research");
    }

    @Test
    void removeRejectsCrossOwner()
    {
        SysFileFolder cur = new SysFileFolder();
        cur.setId(10L);
        cur.setOwner("bob");
        when(folderMapper.selectById(10L)).thenReturn(cur);
        assertThatThrownBy(() -> folderService.remove(10L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("没有权限");
    }
}
