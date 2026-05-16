package com.scaffold.module.cms.workflow.bridge;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.scaffold.module.cms.service.ArticleService;

/**
 * 监听 Flowable {@link FlowableEngineEventType#PROCESS_COMPLETED} 事件 → 反向同步 CMS 文章状态。
 * <p>
 * 工作机制：
 * <ol>
 *   <li>引擎启动后通过 {@link RuntimeService#addEventListener} 自注册</li>
 *   <li>每条流程结束时拿 processDefinitionKey 过滤——只处理 {@code cms_article_review}</li>
 *   <li>从历史变量里拿 {@code articleId / approved / reason}，决定走 onWorkflowApprove 还是 onWorkflowReject</li>
 *   <li>actor（最后一个完成审核任务的用户）：从 historic process instance 的 startUserId 退化（不能拿到 last task 的 owner，
 *       因为 PROCESS_COMPLETED 时 task 已结束）；本地 admin / Flowable Authentication 兜底</li>
 * </ol>
 *
 * <p>这个 listener 只在 {@link CmsWorkflowBridgeAutoConfiguration} 装配时被实例化——
 * 桥模块未启用 / cms 或 workflow 模块缺失时整段代码不进入 Spring 容器，自然不会订阅。</p>
 */
@Component
public class ArticleWorkflowEventListener implements FlowableEventListener
{
    private static final Logger log = LoggerFactory.getLogger(ArticleWorkflowEventListener.class);

    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final ArticleService articleService;

    public ArticleWorkflowEventListener(RuntimeService runtimeService,
                                        HistoryService historyService,
                                        ArticleService articleService)
    {
        this.runtimeService = runtimeService;
        this.historyService = historyService;
        this.articleService = articleService;
    }

    @PostConstruct
    public void register()
    {
        runtimeService.addEventListener(this, FlowableEngineEventType.PROCESS_COMPLETED);
        log.info("ArticleWorkflowEventListener registered for PROCESS_COMPLETED (cms_article_review)");
    }

    @Override
    public void onEvent(FlowableEvent event)
    {
        if (!(event instanceof FlowableEntityEvent entityEvent)) return;
        Object entity = entityEvent.getEntity();
        if (!(entity instanceof ProcessInstance pi)) return;

        // 只处理 cms 流程；其他业务流程结束不影响 CMS
        if (!WorkflowAwareCmsAdapter.DEF_KEY.equals(extractKey(pi.getProcessDefinitionId()))) return;

        try
        {
            handleCompletion(pi.getId());
        }
        catch (Exception ex)
        {
            log.error("反向同步 CMS 文章状态失败 piid={} reason={}", pi.getId(), ex.getMessage(), ex);
        }
    }

    private void handleCompletion(String processInstanceId)
    {
        HistoricProcessInstance hist = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId).singleResult();
        if (hist == null)
        {
            log.warn("PROCESS_COMPLETED 但找不到历史实例 piid={}", processInstanceId);
            return;
        }

        Long articleId = parseLong(readVar(processInstanceId, WorkflowAwareCmsAdapter.VAR_ARTICLE_ID));
        if (articleId == null && hist.getBusinessKey() != null)
        {
            articleId = parseLong(hist.getBusinessKey());
        }
        if (articleId == null)
        {
            log.warn("无法解析 articleId，跳过反向同步 piid={} businessKey={}",
                    processInstanceId, hist.getBusinessKey());
            return;
        }

        Object approvedVar = readVar(processInstanceId, WorkflowAwareCmsAdapter.VAR_APPROVED);
        boolean approved = parseBool(approvedVar);
        String reason = stringOf(readVar(processInstanceId, WorkflowAwareCmsAdapter.VAR_REASON));

        // actor：流程完成时已经没有 active task，退化用 startUserId（提交人）兜底
        // 真正的"审核者 userId"应该被审核 task 的 complete 调用方记到流程变量里
        // 这里读 task local 已经不可用——退化方案是看流程变量 reviewer
        String actor = stringOf(readVar(processInstanceId, "reviewer"));
        if (actor == null) actor = hist.getStartUserId();

        if (approved)
        {
            articleService.onWorkflowApprove(articleId, actor);
        }
        else
        {
            articleService.onWorkflowReject(articleId, reason, actor);
        }
    }

    private Object readVar(String piid, String name)
    {
        try
        {
            var v = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(piid)
                    .variableName(name)
                    .singleResult();
            return v == null ? null : v.getValue();
        }
        catch (Exception ignore)
        {
            return null;
        }
    }

    private static String extractKey(String processDefinitionId)
    {
        if (processDefinitionId == null) return null;
        int idx = processDefinitionId.indexOf(':');
        return idx > 0 ? processDefinitionId.substring(0, idx) : processDefinitionId;
    }

    private static Long parseLong(Object v)
    {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(v.toString().trim()); }
        catch (Exception ignore) { return null; }
    }

    private static boolean parseBool(Object v)
    {
        if (v == null) return false;
        if (v instanceof Boolean b) return b;
        String s = v.toString().trim().toLowerCase();
        return "true".equals(s) || "1".equals(s) || "yes".equals(s) || "y".equals(s);
    }

    private static String stringOf(Object v)
    {
        if (v == null) return null;
        String s = v.toString();
        return s.isBlank() ? null : s;
    }

    /** 流程变量名 reviewer 用于"是谁完成的最后一步审批"——可在 BPMN 审核节点的 listener 写入，或前端 complete 时塞入。 */
    public static final String VAR_REVIEWER = "reviewer";

    /**
     * 从 Map 中读 articleId / approved / reason / reviewer。
     * 暴露给 controller 层完成 task 时统一字段名（避免 BPMN 与 CMS 字段约定漂移）。
     */
    public static Map<String, Object> buildCompleteVars(boolean approved, String reason, String reviewerUserId)
    {
        Map<String, Object> vars = new java.util.HashMap<>();
        vars.put(WorkflowAwareCmsAdapter.VAR_APPROVED, approved);
        if (reason != null) vars.put(WorkflowAwareCmsAdapter.VAR_REASON, reason);
        if (reviewerUserId != null) vars.put(VAR_REVIEWER, reviewerUserId);
        return vars;
    }

    @Override
    public boolean isFailOnException() { return false; }

    @Override
    public boolean isFireOnTransactionLifecycleEvent() { return false; }

    @Override
    public String getOnTransaction() { return null; }
}
