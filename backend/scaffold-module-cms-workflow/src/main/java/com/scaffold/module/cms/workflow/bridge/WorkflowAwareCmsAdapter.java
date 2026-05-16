package com.scaffold.module.cms.workflow.bridge;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import com.scaffold.common.exception.ServiceException;
import com.scaffold.module.cms.domain.Article;
import com.scaffold.module.cms.mapper.ArticleMapper;
import com.scaffold.module.cms.workflow.CmsWorkflowAdapter;
import com.scaffold.module.workflow.dto.ProcessInstanceView;
import com.scaffold.module.workflow.dto.StartProcessRequest;
import com.scaffold.module.workflow.service.WorkflowFacade;

/**
 * 把 CMS 的"提交审核"接到 Flowable 引擎的真实 {@link CmsWorkflowAdapter} 实现。<br>
 * 仅在 {@link CmsWorkflowBridgeAutoConfiguration} 装配时才生效；
 * 装配生效会用 {@link ConditionalOnMissingBean} 覆盖 cms 模块自带的
 * {@code DefaultCmsWorkflowAdapter}（空实现）。
 *
 * <p>核心契约：
 * <ul>
 *   <li>{@link #onSubmit(Long, String)} 会启动一条 key=cms_article_review 的流程实例，
 *       把 article id 作为 businessKey；流程变量 {@code articleId / authorUserId / submitter}
 *       供审核节点使用</li>
 *   <li>启动后立刻把 processInstanceId 回写到 cms_article 表（新加的列），
 *       同时把 article.status 切到 PENDING（与自闭环路径状态结果一致）</li>
 *   <li>返回 true 让 ArticleService 跳过自闭环 setStatus；事件发送由 ArticleService 兜底</li>
 *   <li>{@link #onApprove(Long)} / {@link #onReject(Long, String)}：
 *       仅在前端"绕过 workflow"通过文章 ReviewBar 直接通过 / 驳回时被调用，
 *       此时桥模块需要清理掉对应流程（cancel 未完成实例），避免引擎里残留任务</li>
 * </ul>
 *
 * <p>BPMN 资源：见 {@link ArticleProcessDeployer}（启动时自动检测 + 部署）。</p>
 */
public class WorkflowAwareCmsAdapter implements CmsWorkflowAdapter
{
    private static final Logger log = LoggerFactory.getLogger(WorkflowAwareCmsAdapter.class);

    /** Flowable 流程定义 key，与自带 BPMN 文件一致；前端 ProcessProgressDialog 的 def lookup 也用它 */
    public static final String DEF_KEY = "cms_article_review";

    /** 流程变量名：业务作者 userId（从 article.create_by 推断；reviewer 节点可用 candidateUser/Group） */
    public static final String VAR_AUTHOR = "authorUserId";

    /** 流程变量名：提交人 userId（与 author 通常相同；区分场景：编辑代提交） */
    public static final String VAR_SUBMITTER = "submitter";

    /** 流程变量名：业务 articleId（即 businessKey 重复一份在变量里，方便事件监听器读） */
    public static final String VAR_ARTICLE_ID = "articleId";

    /** 流程变量名：审核结果（true=通过，false=驳回）；由审核 task complete 时传入 */
    public static final String VAR_APPROVED = "approved";

    /** 流程变量名：审核 reason（驳回时必填，通过可空） */
    public static final String VAR_REASON = "reason";

    @Autowired private WorkflowFacade workflowFacade;
    @Autowired private ArticleMapper articleMapper;

    @Override
    @Transactional
    public boolean onSubmit(Long articleId, String userId)
    {
        if (articleId == null) throw new ServiceException("articleId 不能为空");

        Article article = articleMapper.selectById(articleId);
        if (article == null) throw new ServiceException("文章不存在或已删除: " + articleId);

        // 已经在审中（重复 submit 应该早被 service 层 status 校验拦掉，这里二次保险）
        if (article.getProcessInstanceId() != null && !article.getProcessInstanceId().isBlank())
        {
            log.warn("文章已存在流程实例，跳过启动 articleId={} piid={}",
                    articleId, article.getProcessInstanceId());
            return true;
        }

        StartProcessRequest req = new StartProcessRequest();
        req.setProcessDefinitionKey(DEF_KEY);
        req.setBusinessKey(String.valueOf(articleId));
        req.setName("文章审核：" + article.getTitle());
        Map<String, Object> vars = new HashMap<>();
        vars.put(VAR_ARTICLE_ID, articleId);
        vars.put(VAR_AUTHOR, article.getCreateBy());
        vars.put(VAR_SUBMITTER, userId);
        req.setVariables(vars);

        ProcessInstanceView instance;
        try
        {
            instance = workflowFacade.startProcess(req, userId);
        }
        catch (Exception ex)
        {
            // 流程定义未部署 / 参数校验失败等：不让 CMS 自闭环兜底，而是直接抛错让前端可见。
            // 否则会出现"没人知道流程没起来，但状态变 PENDING 了"的诡异现象。
            log.error("启动 cms_article_review 流程失败 articleId={} reason={}",
                    articleId, ex.getMessage(), ex);
            throw new ServiceException("启动审批流程失败: " + ex.getMessage());
        }

        String piid = instance.getId();
        articleMapper.updateProcessInstanceId(articleId, piid);

        // 直接把 status 切到 PENDING（与自闭环对齐）。
        // 审核节点完成后由 ArticleWorkflowEventListener 反向同步切到 PUBLISHED / DRAFT。
        articleMapper.updateStatus(articleId, Article.STATUS_PENDING, article.getPublishedAt(), userId);

        log.info("CMS 提交审核 → workflow 已启动 articleId={} piid={} actor={}",
                articleId, piid, userId);
        return true;
    }

    /**
     * 前端绕过 workflow 直接通过 / 驳回时的反向调用（M-4-3 决策：两个入口都可驳回）。<br>
     * 这里只清理掉未完成的 Flowable 流程实例，避免引擎里留下"无主任务"。
     * 真正的状态切换由 ArticleService 自己完成。
     */
    @Override
    @Transactional
    public void onApprove(Long articleId)
    {
        cancelPendingInstanceIfAny(articleId, "CMS 文章 ReviewBar 直接通过");
    }

    @Override
    @Transactional
    public void onReject(Long articleId, String reason)
    {
        cancelPendingInstanceIfAny(articleId,
                "CMS 文章 ReviewBar 直接驳回" + (reason == null ? "" : "：" + reason));
    }

    private void cancelPendingInstanceIfAny(Long articleId, String cancelReason)
    {
        Article article = articleMapper.selectById(articleId);
        if (article == null) return;
        String piid = article.getProcessInstanceId();
        if (piid == null || piid.isBlank()) return;
        try
        {
            workflowFacade.cancelInstance(piid, cancelReason);
            log.info("已取消 article 关联的 workflow 实例 articleId={} piid={} reason={}",
                    articleId, piid, cancelReason);
        }
        catch (Exception ex)
        {
            // 实例可能已经结束 / 删除了；记 warn 不阻断业务
            log.warn("取消 workflow 实例失败 articleId={} piid={} reason={}",
                    articleId, piid, ex.getMessage());
        }
    }
}
