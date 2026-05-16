package com.scaffold.module.cms.workflow;

/**
 * CMS 与工作流的解耦适配器。<br>
 * CMS 模块本体**不依赖** scaffold-module-workflow——这里只声明语义接口，
 * 由可选桥模块 {@code scaffold-module-cms-workflow} 提供真正实现：
 * <ul>
 *   <li>{@link #onSubmit(Long, String)}：CMS 文章提交审核 → 桥模块启动 Flowable 流程实例</li>
 *   <li>{@link #onApprove(Long)}：桥模块从 Flowable 接到"通过"事件时回调，
 *       目前仅给桥模块作为"已经处理"的标记，真正状态切换由桥模块直接调
 *       {@code ArticleService.onWorkflowApprove} 完成</li>
 *   <li>{@link #onReject(Long, String)}：同上，"驳回"事件通知点</li>
 * </ul>
 * 默认实现 {@link DefaultCmsWorkflowAdapter} 全部为 no-op + 返回 false，
 * 让 CMS 走自闭环状态机；删 workflow jar 不会让 CMS 编译挂掉。
 */
public interface CmsWorkflowAdapter
{
    /**
     * 文章发起审核。
     *
     * @param articleId 文章 id
     * @param userId 提交人 id
     * @return true 表示已交由外部工作流处理（CMS 本地不再写状态）；
     *         false 表示外部未处理，CMS 自闭环把状态切到 PENDING。
     */
    boolean onSubmit(Long articleId, String userId);

    /**
     * 文章 PENDING 期间被人工切到 PUBLISHED（通过 ReviewBar"通过"按钮）。
     * 桥模块若已经接管了流程，这里会被调用以清理 / 同步流程实例（如取消未完成 task）。
     * 默认空实现什么都不做。
     */
    default void onApprove(Long articleId) { /* no-op */ }

    /**
     * 同上，文章被人工驳回。reason 来自 ReviewBar 输入。
     * 默认空实现什么都不做。
     */
    default void onReject(Long articleId, String reason) { /* no-op */ }
}
