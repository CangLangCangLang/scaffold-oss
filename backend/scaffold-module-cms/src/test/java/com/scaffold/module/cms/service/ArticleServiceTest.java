package com.scaffold.module.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
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
import com.scaffold.module.cms.domain.Article;
import com.scaffold.module.cms.domain.Channel;
import com.scaffold.module.cms.dto.ArticleSaveRequest;
import com.scaffold.module.cms.mapper.ArticleMapper;
import com.scaffold.module.cms.mapper.ChannelMapper;
import com.scaffold.module.cms.mapper.TagMapper;
import com.scaffold.module.cms.workflow.CmsWorkflowAdapter;

/**
 * ArticleService 单测：用 Mockito 替代 mapper 与 workflow adapter，
 * 仅验证业务规则（CRUD 字段映射、状态机校验、slug 生成 + 唯一化、软删 + 关联清理）。
 */
class ArticleServiceTest
{
    private ArticleMapper articleMapper;
    private ChannelMapper channelMapper;
    private TagMapper tagMapper;
    private CmsWorkflowAdapter workflowAdapter;
    private org.springframework.context.ApplicationEventPublisher eventPublisher;
    private ArticleService articleService;

    @BeforeEach
    void setUp()
    {
        articleMapper = mock(ArticleMapper.class);
        channelMapper = mock(ChannelMapper.class);
        tagMapper = mock(TagMapper.class);
        workflowAdapter = mock(CmsWorkflowAdapter.class);
        eventPublisher = mock(org.springframework.context.ApplicationEventPublisher.class);

        articleService = new ArticleService();
        org.springframework.test.util.ReflectionTestUtils.setField(articleService, "articleMapper", articleMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(articleService, "channelMapper", channelMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(articleService, "tagMapper", tagMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(articleService, "workflowAdapter", workflowAdapter);
        org.springframework.test.util.ReflectionTestUtils.setField(articleService, "eventPublisher", eventPublisher);

        // SecurityUtils.getUsername() 默认走 SecurityContextHolder，这里塞一个 LoginUser
        SysUser sys = new SysUser();
        sys.setUserId(1L);
        sys.setUserName("test-actor");
        LoginUser u = new LoginUser(1L, 1L, sys, null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(u, null, Collections.emptyList()));

        Channel ch = new Channel();
        ch.setId(10L);
        ch.setName("news");
        ch.setCode("news");
        when(channelMapper.selectById(eq(10L))).thenReturn(ch);
    }

    @AfterEach
    void tearDown()
    {
        SecurityContextHolder.clearContext();
    }

    /* ===== 创建 / slug 生成 ===== */

    @Test
    void createArticleAssignsDraftAndAutoSlugWhenSlugBlank()
    {
        ArticleSaveRequest req = new ArticleSaveRequest();
        req.setChannelId(10L);
        req.setTitle("Hello World!");

        when(articleMapper.selectBySlug(any())).thenReturn(null);
        when(articleMapper.insert(any())).thenAnswer(inv -> { ((Article) inv.getArgument(0)).setId(100L); return 1; });
        Article saved = sampleSavedArticle(100L, "Hello World!", "hello-world", Article.STATUS_DRAFT, 10L);
        when(articleMapper.selectById(eq(100L))).thenReturn(saved);
        when(articleMapper.selectTagIdsByArticleId(eq(100L))).thenReturn(Collections.emptyList());

        Article res = articleService.save(req);

        assertThat(res.getStatus()).isEqualTo(Article.STATUS_DRAFT);
        assertThat(res.getSlug()).isEqualTo("hello-world");
        verify(articleMapper).insert(any());
        verify(articleMapper, never()).updateById(any());
    }

    @Test
    void createArticleFallsBackToUuidSlugWhenTitleAllNonAscii()
    {
        ArticleSaveRequest req = new ArticleSaveRequest();
        req.setChannelId(10L);
        req.setTitle("欢迎使用");

        when(articleMapper.selectBySlug(any())).thenReturn(null);
        when(articleMapper.insert(any())).thenAnswer(inv -> {
            Article a = inv.getArgument(0);
            a.setId(101L);
            assertThat(a.getSlug()).startsWith("article-");
            return 1;
        });
        Article saved = sampleSavedArticle(101L, "欢迎使用", "article-12345678", Article.STATUS_DRAFT, 10L);
        when(articleMapper.selectById(eq(101L))).thenReturn(saved);
        when(articleMapper.selectTagIdsByArticleId(eq(101L))).thenReturn(Collections.emptyList());

        articleService.save(req);
    }

    @Test
    void createArticleResolvesSlugCollisionByAppendingSequence()
    {
        ArticleSaveRequest req = new ArticleSaveRequest();
        req.setChannelId(10L);
        req.setTitle("hello");
        req.setSlug("hello-world");

        Article occupied = new Article();
        occupied.setId(50L);
        occupied.setSlug("hello-world");
        when(articleMapper.selectBySlug(eq("hello-world"))).thenReturn(occupied);
        when(articleMapper.selectBySlug(eq("hello-world-2"))).thenReturn(null);
        when(articleMapper.insert(any())).thenAnswer(inv -> {
            Article a = inv.getArgument(0);
            a.setId(102L);
            assertThat(a.getSlug()).isEqualTo("hello-world-2");
            return 1;
        });
        Article saved = sampleSavedArticle(102L, "hello", "hello-world-2", Article.STATUS_DRAFT, 10L);
        when(articleMapper.selectById(eq(102L))).thenReturn(saved);
        when(articleMapper.selectTagIdsByArticleId(eq(102L))).thenReturn(Collections.emptyList());

        articleService.save(req);
    }

    @Test
    void createArticleRejectsWhenChannelMissing()
    {
        ArticleSaveRequest req = new ArticleSaveRequest();
        req.setChannelId(999L);
        req.setTitle("foo");
        when(channelMapper.selectById(eq(999L))).thenReturn(null);

        assertThatThrownBy(() -> articleService.save(req))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("栏目不存在");
    }

    /* ===== 状态机：六个动作 ===== */

    @Test
    void submitDraftToPendingWhenWorkflowAdapterDeclines()
    {
        Article draft = sampleSavedArticle(1L, "t", "t", Article.STATUS_DRAFT, 10L);
        when(articleMapper.selectById(eq(1L))).thenReturn(draft);
        when(articleMapper.selectTagIdsByArticleId(eq(1L))).thenReturn(Collections.emptyList());
        when(workflowAdapter.onSubmit(eq(1L), any())).thenReturn(false);

        articleService.submit(1L, "u-1");

        verify(articleMapper).updateStatus(eq(1L), eq(Article.STATUS_PENDING), any(), any());
    }

    @Test
    void submitWithWorkflowAdapterTakesOverDoesNotChangeStatusLocally()
    {
        Article draft = sampleSavedArticle(1L, "t", "t", Article.STATUS_DRAFT, 10L);
        when(articleMapper.selectById(eq(1L))).thenReturn(draft);
        when(articleMapper.selectTagIdsByArticleId(eq(1L))).thenReturn(Collections.emptyList());
        when(workflowAdapter.onSubmit(eq(1L), any())).thenReturn(true);

        articleService.submit(1L, "u-1");

        verify(articleMapper, never()).updateStatus(anyLong(), any(), any(), any());
    }

    @Test
    void submitFromNonDraftIsRejected()
    {
        Article a = sampleSavedArticle(1L, "t", "t", Article.STATUS_PENDING, 10L);
        when(articleMapper.selectById(eq(1L))).thenReturn(a);
        when(articleMapper.selectTagIdsByArticleId(eq(1L))).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> articleService.submit(1L, "u-1"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("仅草稿");
    }

    @Test
    void approveSetsPublishedAndStampsPublishedAtFirstTime()
    {
        Article a = sampleSavedArticle(1L, "t", "t", Article.STATUS_PENDING, 10L);
        a.setPublishedAt(null);
        when(articleMapper.selectById(eq(1L))).thenReturn(a);
        when(articleMapper.selectTagIdsByArticleId(eq(1L))).thenReturn(Collections.emptyList());

        articleService.approve(1L);

        verify(articleMapper).updateStatus(eq(1L), eq(Article.STATUS_PUBLISHED),
                org.mockito.ArgumentMatchers.argThat(d -> d != null), any());
    }

    @Test
    void rejectGoesPendingToDraft()
    {
        Article a = sampleSavedArticle(1L, "t", "t", Article.STATUS_PENDING, 10L);
        when(articleMapper.selectById(eq(1L))).thenReturn(a);
        when(articleMapper.selectTagIdsByArticleId(eq(1L))).thenReturn(Collections.emptyList());

        articleService.reject(1L);

        verify(articleMapper).updateStatus(eq(1L), eq(Article.STATUS_DRAFT), any(), any());
    }

    @Test
    void unpublishGoesPublishedToUnpublished()
    {
        Article a = sampleSavedArticle(1L, "t", "t", Article.STATUS_PUBLISHED, 10L);
        when(articleMapper.selectById(eq(1L))).thenReturn(a);
        when(articleMapper.selectTagIdsByArticleId(eq(1L))).thenReturn(Collections.emptyList());

        articleService.unpublish(1L);

        verify(articleMapper).updateStatus(eq(1L), eq(Article.STATUS_UNPUBLISHED), any(), any());
    }

    @Test
    void republishKeepsOriginalPublishedAt()
    {
        java.util.Date original = new java.util.Date(1_700_000_000_000L);
        Article a = sampleSavedArticle(1L, "t", "t", Article.STATUS_UNPUBLISHED, 10L);
        a.setPublishedAt(original);
        when(articleMapper.selectById(eq(1L))).thenReturn(a);
        when(articleMapper.selectTagIdsByArticleId(eq(1L))).thenReturn(Collections.emptyList());

        articleService.republish(1L);

        verify(articleMapper).updateStatus(eq(1L), eq(Article.STATUS_PUBLISHED),
                eq(original), any());
    }

    @Test
    void backToDraftRejectsWhenAlreadyDraft()
    {
        Article a = sampleSavedArticle(1L, "t", "t", Article.STATUS_DRAFT, 10L);
        when(articleMapper.selectById(eq(1L))).thenReturn(a);
        when(articleMapper.selectTagIdsByArticleId(eq(1L))).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> articleService.backToDraft(1L))
                .isInstanceOf(ServiceException.class);
    }

    /* ===== 软删 ===== */

    @Test
    void deleteSoftDeletesAndClearsTagAssoc()
    {
        Article a = sampleSavedArticle(1L, "t", "t", Article.STATUS_DRAFT, 10L);
        when(articleMapper.selectById(eq(1L))).thenReturn(a);
        when(articleMapper.selectTagIdsByArticleId(eq(1L))).thenReturn(List.of(7L));

        articleService.delete(1L);

        verify(articleMapper).deleteTagsByArticleId(eq(1L));
        verify(articleMapper).softDelete(eq(1L), any());
    }

    /* ===== 列表回填 tags ===== */

    @Test
    void publicPageOnlyFiltersPublishedAndFillsTags()
    {
        when(articleMapper.selectPublicList(any())).thenReturn(new ArrayList<>(List.of(
                sampleSavedArticle(1L, "x", "x", Article.STATUS_PUBLISHED, 10L)
        )));
        when(articleMapper.selectArticleTagPairs(any())).thenReturn(Collections.emptyList());

        articleService.publicPage(new com.scaffold.module.cms.dto.ArticleQuery(), 1, 20);

        verify(articleMapper, atLeastOnce()).selectPublicList(
                org.mockito.ArgumentMatchers.argThat(q -> Article.STATUS_PUBLISHED.equals(q.getStatus())));
    }

    /* ===== 工具 ===== */

    private static Article sampleSavedArticle(Long id, String title, String slug, String status, Long channelId)
    {
        Article a = new Article();
        a.setId(id);
        a.setTitle(title);
        a.setSlug(slug);
        a.setStatus(status);
        a.setChannelId(channelId);
        a.setViewCount(0L);
        return a;
    }

    @Test
    void slugGeneratorTrimsLeadingAndTrailingDashesAndNormalizesUnicode()
    {
        assertThat(ArticleService.generateSlug("  Café — résumé  "))
                .isEqualTo("cafe-resume");
        assertThat(ArticleService.generateSlug("hello world"))
                .isEqualTo("hello-world");
        assertThat(ArticleService.generateSlug("中文标题"))
                .startsWith("article-");
    }

    @Test
    void validateTransitionSpansTheFullStateMachine()
    {
        ArticleService.validateTransition(Article.STATUS_DRAFT, Article.STATUS_PENDING);
        ArticleService.validateTransition(Article.STATUS_PENDING, Article.STATUS_PUBLISHED);
        ArticleService.validateTransition(Article.STATUS_PUBLISHED, Article.STATUS_UNPUBLISHED);
        ArticleService.validateTransition(Article.STATUS_UNPUBLISHED, Article.STATUS_PUBLISHED);
        ArticleService.validateTransition(Article.STATUS_PENDING, Article.STATUS_DRAFT);
        ArticleService.validateTransition(Article.STATUS_PUBLISHED, Article.STATUS_DRAFT);
        // 同状态不算流转，不抛异常
        ArticleService.validateTransition(Article.STATUS_DRAFT, Article.STATUS_DRAFT);

        assertThatThrownBy(() ->
                ArticleService.validateTransition(Article.STATUS_DRAFT, Article.STATUS_PUBLISHED))
                .isInstanceOf(ServiceException.class);
        assertThatThrownBy(() ->
                ArticleService.validateTransition(Article.STATUS_DRAFT, Article.STATUS_UNPUBLISHED))
                .isInstanceOf(ServiceException.class);
    }

    @SuppressWarnings("unused")
    private void unused() { times(1); }
}
