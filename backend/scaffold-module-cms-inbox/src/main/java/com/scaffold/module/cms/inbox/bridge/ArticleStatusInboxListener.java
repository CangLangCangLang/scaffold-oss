package com.scaffold.module.cms.inbox.bridge;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import com.scaffold.framework.web.websocket.bus.MessagePublisher;
import com.scaffold.module.cms.domain.Article;
import com.scaffold.module.cms.event.ArticleStatusChangedEvent;

/**
 * 监听 {@link ArticleStatusChangedEvent} → 给作者发站内信（M-5）。
 *
 * <h3>触发点（只发 b/c/d）</h3>
 * <ul>
 *   <li><b>b)</b> {@code * → PUBLISHED}（审核通过 / 重新上线）：通知<b>作者</b>「【已发布】」</li>
 *   <li><b>c)</b> {@code PENDING → DRAFT}（驳回，reason 非空）：通知<b>作者</b>「【已驳回】原因：xxx」</li>
 *   <li><b>d)</b> {@code PUBLISHED → UNPUBLISHED}（下线）：通知<b>作者</b>「【已下线】」</li>
 * </ul>
 *
 * <h3>不触发的情况</h3>
 * <ul>
 *   <li>a) {@code DRAFT → PENDING}（提交审核）：M-4 启用时由 workflow 模块自带的 TaskNotifyEventListener
 *       通知审核人；M-4 关闭时（CMS 自闭环）没有指定的"审核人"概念，避免群发污染所以也不发</li>
 *   <li>actor == author：用户自己操作自己的文章不打扰自己（如自己下线自己的文章）</li>
 *   <li>{@code DRAFT → DRAFT} 等无意义流转：被前置 ArticleService 的 validateTransition 拦截了，
 *       不会走到这里</li>
 * </ul>
 *
 * <h3>实现细节</h3>
 * <ul>
 *   <li>{@code @Async}：避免同步阻塞 ArticleService 主事务；推送失败不影响 CMS 状态机</li>
 *   <li>{@link MessagePublisher} 通过 {@link ObjectProvider} 软依赖：framework 没装 push bus 时静默跳过</li>
 *   <li>type 命名规则：{@code cms.article.<event-key>}，例如 {@code cms.article.published}，
 *       与 workflow 的 {@code workflow.task.created} 风格一致</li>
 * </ul>
 */
@Component
public class ArticleStatusInboxListener
{
    private static final Logger log = LoggerFactory.getLogger(ArticleStatusInboxListener.class);

    /** 站内信 type 前缀，前端可按 type=cms.article.* 过滤"内容平台"分组。 */
    public static final String TYPE_PREFIX = "cms.article.";

    public static final String TYPE_PUBLISHED = TYPE_PREFIX + "published";
    public static final String TYPE_REJECTED = TYPE_PREFIX + "rejected";
    public static final String TYPE_UNPUBLISHED = TYPE_PREFIX + "unpublished";

    /** 文章管理后台编辑页深链；前端 NotificationBell 看到 link 后 router.push */
    public static final String LINK_TEMPLATE = "/cms/article-edit/%s";

    private final ObjectProvider<MessagePublisher> publisherProvider;

    public ArticleStatusInboxListener(ObjectProvider<MessagePublisher> publisherProvider)
    {
        this.publisherProvider = publisherProvider;
    }

    /**
     * 在 CMS 状态切换事务提交后才触发；CMS 自身回滚时不会发出错误站内信。<br>
     * fallbackExecution=true：即使事件发布在 transaction 之外（例如 service 直接调 publishStatusChanged
     * 而本身不在 @Transactional），仍会同步执行——避免事件被静默丢弃。
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onArticleStatusChanged(ArticleStatusChangedEvent event)
    {
        try
        {
            handle(event);
        }
        catch (Exception ex)
        {
            log.warn("处理 ArticleStatusChangedEvent 失败 articleId={} reason={}",
                    event.getArticleId(), ex.getMessage());
        }
    }

    private void handle(ArticleStatusChangedEvent e)
    {
        MessagePublisher publisher = publisherProvider.getIfAvailable();
        if (publisher == null)
        {
            log.debug("MessagePublisher 不可用，跳过 cms 事件 articleId={}", e.getArticleId());
            return;
        }

        String author = e.getAuthorUserId();
        if (author == null || author.isBlank())
        {
            log.debug("文章无作者信息，跳过 articleId={}", e.getArticleId());
            return;
        }

        // actor == author 时不重复发自己（如自己上线自己的文章）
        if (author.equals(e.getActorUserId()) && !needsSelfNotify(e))
        {
            log.debug("actor==author 跳过 articleId={} author={}", e.getArticleId(), author);
            return;
        }

        String type = pickType(e);
        if (type == null) return; // 不在触发点列表里

        String content = renderContent(e);
        Map<String, Object> payload = buildPayload(e, content);
        try
        {
            publisher.toUser(author, type, payload);
            log.info("CMS inbox notify ✓ to={} type={} articleId={} title={}",
                    author, type, e.getArticleId(), e.getArticleTitle());
        }
        catch (Exception ex)
        {
            log.warn("发送站内信失败 to={} articleId={} reason={}",
                    author, e.getArticleId(), ex.getMessage());
        }
    }

    /**
     * 只在以下三种状态变化时才需要发送站内信。<br>
     * 注意 {@link ArticleStatusChangedEvent} 由 {@code ArticleService.publishStatusChanged} 发布，
     * 已经过 {@code validateTransition} 校验，所以 oldStatus / newStatus 必然合法。
     */
    private static String pickType(ArticleStatusChangedEvent e)
    {
        String to = e.getNewStatus();
        String from = e.getOldStatus();
        if (Article.STATUS_PUBLISHED.equals(to) && !Article.STATUS_PUBLISHED.equals(from))
        {
            return TYPE_PUBLISHED;
        }
        if (Article.STATUS_DRAFT.equals(to) && Article.STATUS_PENDING.equals(from)
                && e.getReason() != null && !e.getReason().isBlank())
        {
            // 驳回必然有 reason；没 reason 的 PENDING→DRAFT 是"运营撤回再编辑"，不打扰作者
            return TYPE_REJECTED;
        }
        if (Article.STATUS_UNPUBLISHED.equals(to) && Article.STATUS_PUBLISHED.equals(from))
        {
            return TYPE_UNPUBLISHED;
        }
        return null;
    }

    private static String renderContent(ArticleStatusChangedEvent e)
    {
        String title = e.getArticleTitle() == null ? ("#" + e.getArticleId()) : e.getArticleTitle();
        String to = e.getNewStatus();
        if (Article.STATUS_PUBLISHED.equals(to))
        {
            return "【已发布】您的文章《" + title + "》已发布上线";
        }
        if (Article.STATUS_DRAFT.equals(to))
        {
            String reason = e.getReason() == null ? "（未填写原因）" : e.getReason();
            return "【已驳回】《" + title + "》原因：" + reason;
        }
        if (Article.STATUS_UNPUBLISHED.equals(to))
        {
            return "【已下线】《" + title + "》已被下线";
        }
        return "您的文章《" + title + "》状态变更：" + e.getOldStatus() + " → " + e.getNewStatus();
    }

    /**
     * 即使 actor == author 也要发的场景：例如「重新上线」自己的文章——这是流程的重要节点，
     * 当前实现未启用此分支（统一不打扰自己）。留接口位是为了将来扩展。
     */
    private static boolean needsSelfNotify(ArticleStatusChangedEvent e)
    {
        return false;
    }

    private static Map<String, Object> buildPayload(ArticleStatusChangedEvent e, String content)
    {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("articleId", e.getArticleId());
        p.put("title", e.getArticleTitle());
        p.put("channelId", e.getChannelId());
        p.put("oldStatus", e.getOldStatus());
        p.put("newStatus", e.getNewStatus());
        p.put("actor", e.getActorUserId());
        p.put("content", content);
        p.put("link", String.format(LINK_TEMPLATE, e.getArticleId()));
        if (e.getReason() != null) p.put("reason", e.getReason());
        if (e.isFromWorkflow()) p.put("processInstanceId", e.getProcessInstanceId());
        return p;
    }
}
