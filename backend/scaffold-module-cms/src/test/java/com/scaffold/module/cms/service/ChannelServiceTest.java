package com.scaffold.module.cms.service;

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
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.scaffold.common.core.domain.entity.SysUser;
import com.scaffold.common.core.domain.model.LoginUser;
import com.scaffold.common.exception.ServiceException;
import com.scaffold.module.cms.domain.Channel;
import com.scaffold.module.cms.dto.ChannelTreeNode;
import com.scaffold.module.cms.mapper.ArticleMapper;
import com.scaffold.module.cms.mapper.ChannelMapper;

class ChannelServiceTest
{
    private ChannelMapper channelMapper;
    private ArticleMapper articleMapper;
    private ChannelService channelService;

    @BeforeEach
    void setUp()
    {
        channelMapper = mock(ChannelMapper.class);
        articleMapper = mock(ArticleMapper.class);
        channelService = new ChannelService();
        org.springframework.test.util.ReflectionTestUtils.setField(channelService, "channelMapper", channelMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(channelService, "articleMapper", articleMapper);

        SysUser sys = new SysUser();
        sys.setUserId(1L);
        sys.setUserName("test-actor");
        LoginUser u = new LoginUser(1L, 1L, sys, null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(u, null, Collections.emptyList()));
    }

    @AfterEach
    void tearDown() { SecurityContextHolder.clearContext(); }

    @Test
    void createRejectsWhenCodeMissing()
    {
        Channel form = new Channel();
        form.setName("x");

        assertThatThrownBy(() -> channelService.create(form))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("code");
    }

    @Test
    void createRejectsWhenCodeAlreadyExists()
    {
        Channel form = new Channel();
        form.setCode("dup");
        form.setName("x");
        Channel exist = new Channel();
        exist.setId(99L);
        exist.setCode("dup");
        when(channelMapper.selectByCode(eq("dup"))).thenReturn(exist);

        assertThatThrownBy(() -> channelService.create(form))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("已存在");
    }

    @Test
    void createSetsDefaultsAndReturnsCreated()
    {
        Channel form = new Channel();
        form.setCode("news");
        form.setName("News");
        when(channelMapper.selectByCode(eq("news"))).thenReturn(null);
        when(channelMapper.insert(any())).thenAnswer(inv -> {
            ((Channel) inv.getArgument(0)).setId(10L);
            return 1;
        });

        Channel out = channelService.create(form);

        assertThat(out.getId()).isEqualTo(10L);
        assertThat(out.getParentId()).isEqualTo(0L);
        assertThat(out.getStatus()).isEqualTo("0");
        assertThat(out.getOrderNum()).isEqualTo(0);
        verify(channelMapper).insert(any());
    }

    @Test
    void deleteRejectsWhenStillHasChildren()
    {
        Channel exist = new Channel();
        exist.setId(1L);
        exist.setName("a");
        when(channelMapper.selectById(eq(1L))).thenReturn(exist);
        when(channelMapper.countByParentId(eq(1L))).thenReturn(2);

        assertThatThrownBy(() -> channelService.delete(1L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("子栏目");
        verify(channelMapper, never()).softDelete(anyLong(), any());
    }

    @Test
    void deleteRejectsWhenStillHasArticles()
    {
        Channel exist = new Channel();
        exist.setId(1L);
        exist.setName("a");
        when(channelMapper.selectById(eq(1L))).thenReturn(exist);
        when(channelMapper.countByParentId(eq(1L))).thenReturn(0);
        when(articleMapper.countByChannelId(eq(1L))).thenReturn(5);

        assertThatThrownBy(() -> channelService.delete(1L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("文章");
    }

    @Test
    void updateRejectsCycleWhenMovingUnderOwnDescendant()
    {
        Channel a = new Channel();
        a.setId(1L);
        a.setParentId(0L);
        a.setCode("a");
        a.setName("A");
        Channel b = new Channel();
        b.setId(2L);
        b.setParentId(1L);
        b.setCode("b");
        b.setName("B");
        when(channelMapper.selectById(eq(1L))).thenReturn(a);
        when(channelMapper.selectById(eq(2L))).thenReturn(b);

        Channel form = new Channel();
        form.setId(1L);
        form.setCode("a");
        form.setName("A");
        form.setParentId(2L); // 把 A 挂到 B 下，但 B 是 A 的子，会形成环

        assertThatThrownBy(() -> channelService.update(form))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("循环");
    }

    @Test
    void treeBuildsByParentIdRelationship()
    {
        Channel root = new Channel();
        root.setId(1L);
        root.setParentId(0L);
        root.setName("Root");
        Channel child = new Channel();
        child.setId(2L);
        child.setParentId(1L);
        child.setName("Child");
        when(channelMapper.selectList(any())).thenReturn(List.of(root, child));

        List<ChannelTreeNode> tree = channelService.tree(false);

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getChildren()).hasSize(1);
        assertThat(tree.get(0).getChildren().get(0).getId()).isEqualTo(2L);
    }
}
