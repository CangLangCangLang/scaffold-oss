package com.scaffold.module.cms.event;

import org.springframework.context.ApplicationEvent;

/**
 * CMS 文章状态变更事件。<br>
 * 在 {@link com.scaffold.module.cms.service.ArticleService} 的状态转换方法中，
 * 事务提交后由 Spring {@code ApplicationEventPublisher} 发布——CMS 本体只发不订阅。
 * <p>
 * 订阅者：
 * <ul>
 *   <li>{@code scaffold-module-cms-workflow}（M-4）：自身不消费此事件，反向同步走 Flowable 监听器</li>
 *   <li>{@code scaffold-module-cms-inbox}（M-5）：消费事件 → 调 MessagePublisher 发站内信</li>
 *   <li>未来还会有报表 / 搜索引擎索引等其他订阅者</li>
 * </ul>
 * <p>
 * 字段：
 * <ul>
 *   <li>{@link #articleId} / {@link #articleTitle}：识别文章；title 缓存进事件免得订阅者再查一遍</li>
 *   <li>{@link #channelId}：栏目 id</li>
 *   <li>{@link #oldStatus} / {@link #newStatus}：流转前后状态</li>
 *   <li>{@link #actorUserId}：发起本次切换的用户 id（提交 / 审核 / 下线 操作者）</li>
 *   <li>{@link #authorUserId}：文章作者；通常等于 article.create_by</li>
 *   <li>{@link #reason}：驳回 / 下线 等需要原因的场景；可空</li>
 *   <li>{@link #processInstanceId}：M-4 启用时桥模块写入；可空</li>
 * </ul>
 */
public class ArticleStatusChangedEvent extends ApplicationEvent
{
    private final Long articleId;
    private final String articleTitle;
    private final Long channelId;
    private final String oldStatus;
    private final String newStatus;
    private final String actorUserId;
    private final String authorUserId;
    private final String reason;
    private final String processInstanceId;

    public ArticleStatusChangedEvent(Object source,
                                     Long articleId,
                                     String articleTitle,
                                     Long channelId,
                                     String oldStatus,
                                     String newStatus,
                                     String actorUserId,
                                     String authorUserId,
                                     String reason,
                                     String processInstanceId)
    {
        super(source);
        this.articleId = articleId;
        this.articleTitle = articleTitle;
        this.channelId = channelId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.actorUserId = actorUserId;
        this.authorUserId = authorUserId;
        this.reason = reason;
        this.processInstanceId = processInstanceId;
    }

    public Long getArticleId() { return articleId; }

    public String getArticleTitle() { return articleTitle; }

    public Long getChannelId() { return channelId; }

    public String getOldStatus() { return oldStatus; }

    public String getNewStatus() { return newStatus; }

    public String getActorUserId() { return actorUserId; }

    public String getAuthorUserId() { return authorUserId; }

    public String getReason() { return reason; }

    public String getProcessInstanceId() { return processInstanceId; }

    /** 是否由 workflow 桥模块发起的状态变更（仅 M-4 启用且文章走过 workflow 时为 true）。 */
    public boolean isFromWorkflow() { return processInstanceId != null && !processInstanceId.isBlank(); }

    @Override
    public String toString()
    {
        return "ArticleStatusChangedEvent{id=" + articleId
                + ", " + oldStatus + "->" + newStatus
                + ", actor=" + actorUserId
                + ", author=" + authorUserId
                + (reason != null ? ", reason=" + reason : "")
                + (processInstanceId != null ? ", piid=" + processInstanceId : "")
                + "}";
    }
}
