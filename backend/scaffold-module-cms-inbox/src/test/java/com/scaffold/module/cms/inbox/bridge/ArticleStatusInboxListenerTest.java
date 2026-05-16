package com.scaffold.module.cms.inbox.bridge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import com.scaffold.framework.web.websocket.bus.MessagePublisher;
import com.scaffold.module.cms.domain.Article;
import com.scaffold.module.cms.event.ArticleStatusChangedEvent;

/**
 * 验证 M-5 ArticleStatusInboxListener 的触发表：
 * <ul>
 *   <li>PENDING → PUBLISHED：发"已发布"给作者（通过/重新上线）</li>
 *   <li>UNPUBLISHED → PUBLISHED：同上（重新上线也是 PUBLISHED）</li>
 *   <li>PENDING → DRAFT 带 reason：发"已驳回"给作者，content 包含原因</li>
 *   <li>PENDING → DRAFT 不带 reason：跳过（视为运营撤回再编辑）</li>
 *   <li>PUBLISHED → UNPUBLISHED：发"已下线"给作者</li>
 *   <li>DRAFT → PENDING（提交审核）：跳过（M-4 接管 / M-4 关闭时也不打扰）</li>
 *   <li>actor == author：跳过（不打扰自己）</li>
 *   <li>publisher 为 null（push bus 缺失）：静默跳过，不抛错</li>
 *   <li>payload 中 link 与 type 字段正确</li>
 * </ul>
 */
class ArticleStatusInboxListenerTest
{
    @SuppressWarnings("unchecked")
    private ObjectProvider<MessagePublisher> publisherProvider = mock(ObjectProvider.class);
    private MessagePublisher publisher = mock(MessagePublisher.class);
    private ArticleStatusInboxListener listener;

    @BeforeEach
    void setUp()
    {
        publisherProvider = mock(ObjectProvider.class);
        publisher = mock(MessagePublisher.class);
        when(publisherProvider.getIfAvailable()).thenReturn(publisher);
        listener = new ArticleStatusInboxListener(publisherProvider);
    }

    private static ArticleStatusChangedEvent event(String oldStatus, String newStatus,
                                                   String actor, String author, String reason)
    {
        return new ArticleStatusChangedEvent("test", 100L, "测试文章",
                7L, oldStatus, newStatus, actor, author, reason, null);
    }

    @Test
    void publishesNotifyOnApproveTransitionPendingToPublished()
    {
        listener.onArticleStatusChanged(
                event(Article.STATUS_PENDING, Article.STATUS_PUBLISHED, "reviewer", "alice", null));

        ArgumentCaptor<String> typeCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payloadCap = ArgumentCaptor.forClass(Object.class);
        verify(publisher).toUser(eq("alice"), typeCap.capture(), payloadCap.capture());
        assertThat(typeCap.getValue()).isEqualTo(ArticleStatusInboxListener.TYPE_PUBLISHED);

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) payloadCap.getValue();
        assertThat(payload.get("link")).isEqualTo("/cms/article-edit/100");
        assertThat(payload.get("content").toString()).contains("已发布").contains("测试文章");
    }

    @Test
    void publishesNotifyOnRepublishTransitionUnpublishedToPublished()
    {
        listener.onArticleStatusChanged(
                event(Article.STATUS_UNPUBLISHED, Article.STATUS_PUBLISHED, "ops", "alice", null));
        verify(publisher).toUser(eq("alice"),
                eq(ArticleStatusInboxListener.TYPE_PUBLISHED), any());
    }

    @Test
    void publishesRejectNotifyWithReason()
    {
        listener.onArticleStatusChanged(
                event(Article.STATUS_PENDING, Article.STATUS_DRAFT, "reviewer", "alice", "标题不合规"));

        ArgumentCaptor<Object> payloadCap = ArgumentCaptor.forClass(Object.class);
        verify(publisher).toUser(eq("alice"),
                eq(ArticleStatusInboxListener.TYPE_REJECTED), payloadCap.capture());
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) payloadCap.getValue();
        assertThat(payload.get("content").toString()).contains("已驳回").contains("标题不合规");
        assertThat(payload.get("reason")).isEqualTo("标题不合规");
    }

    @Test
    void skipsRejectWhenReasonIsBlank()
    {
        listener.onArticleStatusChanged(
                event(Article.STATUS_PENDING, Article.STATUS_DRAFT, "reviewer", "alice", null));
        verify(publisher, never()).toUser(anyString(), anyString(), any());
    }

    @Test
    void publishesUnpublishNotify()
    {
        listener.onArticleStatusChanged(
                event(Article.STATUS_PUBLISHED, Article.STATUS_UNPUBLISHED, "ops", "alice", "过期"));
        verify(publisher).toUser(eq("alice"),
                eq(ArticleStatusInboxListener.TYPE_UNPUBLISHED), any());
    }

    @Test
    void skipsSubmitTransitionDraftToPending()
    {
        // M-4 接管 PENDING 通知；M-4 关闭时 CMS 自闭环也不打扰
        listener.onArticleStatusChanged(
                event(Article.STATUS_DRAFT, Article.STATUS_PENDING, "alice", "alice", null));
        verify(publisher, never()).toUser(anyString(), anyString(), any());
    }

    @Test
    void skipsWhenActorEqualsAuthor()
    {
        listener.onArticleStatusChanged(
                event(Article.STATUS_PUBLISHED, Article.STATUS_UNPUBLISHED, "alice", "alice", "自己下线"));
        verify(publisher, never()).toUser(anyString(), anyString(), any());
    }

    @Test
    void doesNotThrowWhenPublisherUnavailable()
    {
        when(publisherProvider.getIfAvailable()).thenReturn(null);
        listener.onArticleStatusChanged(
                event(Article.STATUS_PENDING, Article.STATUS_PUBLISHED, "reviewer", "alice", null));
        // 没抛异常即可；不需要再 verify publisher
    }

    @Test
    void skipsWhenAuthorIsBlank()
    {
        listener.onArticleStatusChanged(
                event(Article.STATUS_PENDING, Article.STATUS_PUBLISHED, "reviewer", "", null));
        verify(publisher, never()).toUser(anyString(), anyString(), any());
    }

    @Test
    void payloadCarriesProcessInstanceIdWhenFromWorkflow()
    {
        ArticleStatusChangedEvent ev = new ArticleStatusChangedEvent("test", 100L, "测试文章",
                7L, Article.STATUS_PENDING, Article.STATUS_PUBLISHED,
                "reviewer", "alice", null, "piid-9");
        listener.onArticleStatusChanged(ev);

        ArgumentCaptor<Object> payloadCap = ArgumentCaptor.forClass(Object.class);
        verify(publisher).toUser(eq("alice"), anyString(), payloadCap.capture());
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) payloadCap.getValue();
        assertThat(payload.get("processInstanceId")).isEqualTo("piid-9");
    }
}
