package com.scaffold.module.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.scaffold.common.core.domain.entity.SysUser;
import com.scaffold.common.core.domain.model.LoginUser;
import com.scaffold.module.cms.domain.Article;
import com.scaffold.module.cms.event.ArticleStatusChangedEvent;
import com.scaffold.module.cms.mapper.ArticleMapper;
import com.scaffold.module.cms.mapper.ChannelMapper;
import com.scaffold.module.cms.mapper.TagMapper;
import com.scaffold.module.cms.workflow.CmsWorkflowAdapter;

/**
 * 验证 M-4 接入后的 ArticleService 行为：
 * <ul>
 *   <li>onWorkflowApprove → PUBLISHED + publishedAt 写入 + 发 ArticleStatusChangedEvent</li>
 *   <li>onWorkflowReject → DRAFT + reason 透传到 Event</li>
 *   <li>onWorkflowApprove 文章状态非 PENDING 时跳过</li>
 *   <li>reject(id, reason) 把 reason 透到 Event</li>
 *   <li>submit() 桥模块 onSubmit 返回 true 时，CMS 不再调 setStatus，但仍发一次状态变化事件（PENDING）</li>
 * </ul>
 */
class ArticleServiceWorkflowCallbackTest
{
    private ArticleMapper articleMapper;
    private ChannelMapper channelMapper;
    private TagMapper tagMapper;
    private CmsWorkflowAdapter workflowAdapter;
    private ApplicationEventPublisher eventPublisher;
    private ArticleService articleService;

    @BeforeEach
    void setUp()
    {
        articleMapper = mock(ArticleMapper.class);
        channelMapper = mock(ChannelMapper.class);
        tagMapper = mock(TagMapper.class);
        workflowAdapter = mock(CmsWorkflowAdapter.class);
        eventPublisher = mock(ApplicationEventPublisher.class);

        articleService = new ArticleService();
        org.springframework.test.util.ReflectionTestUtils.setField(articleService, "articleMapper", articleMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(articleService, "channelMapper", channelMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(articleService, "tagMapper", tagMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(articleService, "workflowAdapter", workflowAdapter);
        org.springframework.test.util.ReflectionTestUtils.setField(articleService, "eventPublisher", eventPublisher);

        SysUser sys = new SysUser();
        sys.setUserId(1L);
        sys.setUserName("test-actor");
        LoginUser u = new LoginUser(1L, 1L, sys, null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(u, null, Collections.emptyList()));
    }

    @AfterEach
    void tearDown()
    {
        SecurityContextHolder.clearContext();
    }

    private Article sample(Long id, String status, java.util.Date publishedAt, String piid)
    {
        Article a = new Article();
        a.setId(id);
        a.setChannelId(99L);
        a.setTitle("文章 " + id);
        a.setStatus(status);
        a.setPublishedAt(publishedAt);
        a.setProcessInstanceId(piid);
        a.setCreateBy("alice");
        return a;
    }

    @Test
    void onWorkflowApproveTransitionsPendingToPublishedAndFiresEvent()
    {
        Article before = sample(11L, Article.STATUS_PENDING, null, "piid-1");
        Article after = sample(11L, Article.STATUS_PUBLISHED, new java.util.Date(), "piid-1");
        when(articleMapper.selectById(eq(11L))).thenReturn(before, after);
        when(articleMapper.selectTagIdsByArticleId(eq(11L))).thenReturn(Collections.emptyList());

        Article res = articleService.onWorkflowApprove(11L, "reviewer-007");

        assertThat(res.getStatus()).isEqualTo(Article.STATUS_PUBLISHED);
        verify(articleMapper).updateStatus(eq(11L), eq(Article.STATUS_PUBLISHED), any(), eq("reviewer-007"));
        // workflow callback 不应再调回 adapter（避免回调闭环）
        verify(workflowAdapter, never()).onApprove(any());

        ArgumentCaptor<ArticleStatusChangedEvent> cap = ArgumentCaptor.forClass(ArticleStatusChangedEvent.class);
        verify(eventPublisher, atLeastOnce()).publishEvent(cap.capture());
        ArticleStatusChangedEvent ev = cap.getValue();
        assertThat(ev.getNewStatus()).isEqualTo(Article.STATUS_PUBLISHED);
        assertThat(ev.getOldStatus()).isEqualTo(Article.STATUS_PENDING);
        assertThat(ev.getActorUserId()).isEqualTo("reviewer-007");
        assertThat(ev.getProcessInstanceId()).isEqualTo("piid-1");
    }

    @Test
    void onWorkflowRejectTransitionsPendingToDraftCarryingReason()
    {
        Article before = sample(12L, Article.STATUS_PENDING, null, "piid-2");
        Article after = sample(12L, Article.STATUS_DRAFT, null, "piid-2");
        when(articleMapper.selectById(eq(12L))).thenReturn(before, after);
        when(articleMapper.selectTagIdsByArticleId(eq(12L))).thenReturn(Collections.emptyList());

        articleService.onWorkflowReject(12L, "标题不合规", "reviewer-007");

        verify(articleMapper).updateStatus(eq(12L), eq(Article.STATUS_DRAFT), any(), eq("reviewer-007"));
        ArgumentCaptor<ArticleStatusChangedEvent> cap = ArgumentCaptor.forClass(ArticleStatusChangedEvent.class);
        verify(eventPublisher).publishEvent(cap.capture());
        ArticleStatusChangedEvent ev = cap.getValue();
        assertThat(ev.getReason()).isEqualTo("标题不合规");
        assertThat(ev.getOldStatus()).isEqualTo(Article.STATUS_PENDING);
        assertThat(ev.getNewStatus()).isEqualTo(Article.STATUS_DRAFT);
    }

    @Test
    void onWorkflowApproveSkipsWhenNotPending()
    {
        Article before = sample(13L, Article.STATUS_DRAFT, null, null);
        when(articleMapper.selectById(eq(13L))).thenReturn(before);
        when(articleMapper.selectTagIdsByArticleId(eq(13L))).thenReturn(Collections.emptyList());

        Article res = articleService.onWorkflowApprove(13L, "reviewer-007");
        assertThat(res.getStatus()).isEqualTo(Article.STATUS_DRAFT);
        verify(articleMapper, never()).updateStatus(eq(13L), anyString(), any(), anyString());
        verify(eventPublisher, never()).publishEvent(any(ArticleStatusChangedEvent.class));
    }

    @Test
    void rejectViaReviewBarPassesReasonToAdapterAndEvent()
    {
        Article before = sample(14L, Article.STATUS_PENDING, null, null);
        Article after = sample(14L, Article.STATUS_DRAFT, null, null);
        when(articleMapper.selectById(eq(14L))).thenReturn(before, after, after, after);
        when(articleMapper.selectTagIdsByArticleId(eq(14L))).thenReturn(Collections.emptyList());

        articleService.reject(14L, "正文太短");

        verify(workflowAdapter).onReject(eq(14L), eq("正文太短"));
        ArgumentCaptor<ArticleStatusChangedEvent> cap = ArgumentCaptor.forClass(ArticleStatusChangedEvent.class);
        verify(eventPublisher).publishEvent(cap.capture());
        assertThat(cap.getValue().getReason()).isEqualTo("正文太短");
    }

    @Test
    void submitWhenWorkflowAdapterTakesOverDoesNotCallSetStatusButStillPublishesEvent()
    {
        Article draft = sample(15L, Article.STATUS_DRAFT, null, null);
        Article pending = sample(15L, Article.STATUS_PENDING, null, "piid-15");
        // adapter 接管：先返回 DRAFT 用于 status 校验，再返回 PENDING（adapter 已经写状态 + piid）
        when(articleMapper.selectById(eq(15L))).thenReturn(draft, pending, pending, pending);
        when(articleMapper.selectTagIdsByArticleId(eq(15L))).thenReturn(Collections.emptyList());
        when(workflowAdapter.onSubmit(eq(15L), anyString())).thenReturn(true);

        Article res = articleService.submit(15L, "alice");
        assertThat(res.getStatus()).isEqualTo(Article.STATUS_PENDING);
        // adapter 接管时 service 不应该再走自闭环 setStatus
        verify(articleMapper, never()).updateStatus(eq(15L), eq(Article.STATUS_PENDING), isNull(), anyString());
        // 但事件仍然发出去（让 inbox 桥能感知 PENDING）
        verify(eventPublisher, times(1)).publishEvent(any(ArticleStatusChangedEvent.class));
    }
}
