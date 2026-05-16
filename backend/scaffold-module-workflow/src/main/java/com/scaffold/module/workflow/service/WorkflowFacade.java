package com.scaffold.module.workflow.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.task.Comment;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.scaffold.common.exception.ServiceException;
import com.scaffold.framework.web.websocket.bus.MessagePublisher;
import com.scaffold.module.workflow.dto.AddSignBeforeRequest;
import com.scaffold.module.workflow.dto.AddSignRequest;
import com.scaffold.module.workflow.dto.CcRequest;
import com.scaffold.module.workflow.dto.CompleteTaskRequest;
import com.scaffold.module.workflow.dto.ProcessDefinitionView;
import com.scaffold.module.workflow.dto.ProcessInstanceView;
import com.scaffold.module.workflow.dto.ProcessRuntimeStateView;
import com.scaffold.module.workflow.dto.SendBackRequest;
import com.scaffold.module.workflow.dto.StartProcessRequest;
import com.scaffold.module.workflow.dto.TaskView;
import com.scaffold.module.workflow.dto.TimelineEntry;

/**
 * 工作流业务封装：不让 Controller 直接接触 Flowable 的引擎服务，
 * 同时屏蔽 Flowable 8 的 API 演进。
 *
 * @author scaffold
 */
@Service
public class WorkflowFacade
{
    private static final Logger log = LoggerFactory.getLogger(WorkflowFacade.class);

    /** 流程变量名：曾经被退回过的 activity id 集合，用于 BPMN 流程图叠色 */
    public static final String VAR_REJECTED_ACTIVITY_IDS = "scaffoldRejectedActivityIds";

    /** 流程变量名：抄送历史，元素结构 {fromUserId, toUserId, comment, occurredAt, taskId} */
    public static final String VAR_CC_HISTORY = "scaffoldCcHistory";

    /** 流程变量名：后加签历史，元素结构 {originTaskId, addedAssignee, comment, occurredAt, mode='AFTER'} */
    public static final String VAR_ADDSIGN_HISTORY = "scaffoldAddSignHistory";

    /** 流程变量名：前加签历史，元素结构 {originTaskId, addedAssignee, childTaskId, comment, occurredAt, mode='BEFORE'} */
    public static final String VAR_ADDSIGN_BEFORE_HISTORY = "scaffoldAddSignBeforeHistory";

    /**
     * 任务局部变量名：原任务被前加签阻塞期间，记录所有阻塞它的子任务 id 列表。
     * 子任务完成时该列表会减一，全部清空后允许原任务提交。
     */
    public static final String VAR_BLOCKED_BY_TASK_IDS = "scaffoldBlockedByTaskIds";

    /** 任务局部变量名：前加签子任务回指原任务，便于完成时主动清理阻塞。 */
    public static final String VAR_PRESIGN_ORIGIN_TASK_ID = "scaffoldPreSignOriginTaskId";

    /** 任务局部变量名：前加签子任务回指原任务的流程实例 ID（standalone task 没有 piid，需手动记录） */
    public static final String VAR_PRESIGN_ORIGIN_PROCESS_INSTANCE_ID = "scaffoldPreSignOriginProcessInstanceId";

    /** 推送总线类型：抄送通知 */
    public static final String PUSH_TYPE_CC = "workflow.task.cc";

    /** 推送总线类型：前加签通知 */
    public static final String PUSH_TYPE_ADDSIGN_BEFORE = "workflow.task.addsign-before";

    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private HistoryService historyService;

    /**
     * 软依赖：抄送 / 后加签的"通知接收人"，需要 push bus；推送总线模块未启用时 cc / addSign
     * 仍然成功，只是没有站内信，依旧落审计 + 流程变量。
     */
    @Autowired
    private ObjectProvider<MessagePublisher> messagePublisherProvider;

    // -------------- Deployment / ProcessDefinition --------------

    public Deployment deployBpmn(String name, String resourceName, InputStream bpmn)
    {
        return repositoryService.createDeployment()
                .name(name)
                .addInputStream(resourceName, bpmn)
                .deploy();
    }

    public List<ProcessDefinitionView> listLatestProcessDefinitions(String keyword)
    {
        var query = repositoryService.createProcessDefinitionQuery().latestVersion().active();
        if (keyword != null && !keyword.isBlank())
        {
            query = query.processDefinitionNameLike("%" + keyword + "%");
        }
        List<ProcessDefinition> defs = query.orderByProcessDefinitionKey().asc().list();
        List<ProcessDefinitionView> views = new ArrayList<>(defs.size());
        for (ProcessDefinition def : defs)
        {
            ProcessDefinitionView v = new ProcessDefinitionView();
            v.setId(def.getId());
            v.setKey(def.getKey());
            v.setName(def.getName());
            v.setVersion(def.getVersion());
            v.setDescription(def.getDescription());
            v.setResourceName(def.getResourceName());
            v.setDeploymentId(def.getDeploymentId());
            v.setSuspended(def.isSuspended());
            Deployment dep = repositoryService.createDeploymentQuery()
                    .deploymentId(def.getDeploymentId()).singleResult();
            if (dep != null) v.setDeploymentTime(dep.getDeploymentTime());
            views.add(v);
        }
        return views;
    }

    public InputStream getBpmnXml(String processDefinitionId)
    {
        ProcessDefinition def = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(processDefinitionId).singleResult();
        if (def == null) throw new ServiceException("流程定义不存在: " + processDefinitionId);
        return repositoryService.getResourceAsStream(def.getDeploymentId(), def.getResourceName());
    }

    /**
     * 同 key 下的所有历史版本，按 version DESC（新→旧）排，给前端"版本对比"下拉用。
     * 与 {@link #listLatestProcessDefinitions(String)} 相反 —— 后者只返每个 key 的最新一版且过滤 suspended，
     * 这里**全量返回**（含 suspended）以便对比能选到任意历史版本。
     *
     * @param key processDefinitionKey，必填
     * @return 视图列表（id / key / name / version / deploymentTime / ...），不会为 null；key 不存在时返空 list
     */
    public List<ProcessDefinitionView> listVersionsByKey(String key)
    {
        if (key == null || key.isBlank())
        {
            throw new ServiceException("processDefinitionKey 不能为空");
        }
        List<ProcessDefinition> defs = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(key)
                .orderByProcessDefinitionVersion().desc()
                .list();
        List<ProcessDefinitionView> views = new ArrayList<>(defs.size());
        for (ProcessDefinition def : defs)
        {
            ProcessDefinitionView v = new ProcessDefinitionView();
            v.setId(def.getId());
            v.setKey(def.getKey());
            v.setName(def.getName());
            v.setVersion(def.getVersion());
            v.setDescription(def.getDescription());
            v.setResourceName(def.getResourceName());
            v.setDeploymentId(def.getDeploymentId());
            v.setSuspended(def.isSuspended());
            Deployment dep = repositoryService.createDeploymentQuery()
                    .deploymentId(def.getDeploymentId()).singleResult();
            if (dep != null) v.setDeploymentTime(dep.getDeploymentTime());
            views.add(v);
        }
        return views;
    }

    public void deleteDeployment(String deploymentId, boolean cascade)
    {
        repositoryService.deleteDeployment(deploymentId, cascade);
    }

    // -------------- Process Instance --------------

    @Transactional
    public ProcessInstanceView startProcess(StartProcessRequest req, String startUserId)
    {
        if (req.getProcessDefinitionKey() == null || req.getProcessDefinitionKey().isBlank())
        {
            throw new ServiceException("processDefinitionKey 不能为空");
        }
        Map<String, Object> vars = req.getVariables() == null ? new HashMap<>() : new HashMap<>(req.getVariables());
        if (startUserId != null) Authentication.setAuthenticatedUserId(startUserId);
        try
        {
            ProcessInstance pi = runtimeService.startProcessInstanceByKey(
                    req.getProcessDefinitionKey(), req.getBusinessKey(), vars);
            if (req.getName() != null && !req.getName().isBlank())
            {
                runtimeService.setProcessInstanceName(pi.getId(), req.getName());
            }
            return toView(pi);
        }
        finally
        {
            if (startUserId != null) Authentication.setAuthenticatedUserId(null);
        }
    }

    public List<ProcessInstanceView> listMyStartedActiveInstances(String userId)
    {
        if (userId == null) return List.of();
        List<ProcessInstance> list = runtimeService.createProcessInstanceQuery()
                .startedBy(userId).active()
                .orderByStartTime().desc()
                .list();
        return list.stream().map(this::toView).toList();
    }

    /**
     * 流程实例分页查询（admin 视角）。运行时与历史用同一接口：
     * <ul>
     *   <li>{@code status="running"}：runtime instance 查 active</li>
     *   <li>{@code status="finished"}：history instance 查 finished</li>
     *   <li>{@code status} 为 null / "all"：history instance（包含已结束 + 未结束），保证全量</li>
     * </ul>
     * 非 admin 调用方应在 controller 层强制传入 {@code actorUserId}（== currentUserId），
     * 由这里转成 {@code startedBy=actorUserId} 过滤；admin 不传 actorUserId 即看全量。
     *
     * @param defKey 流程定义 key（精确匹配）
     * @param businessKey 业务 key（精确匹配）
     * @param actorUserId 流程发起人 userId（精确匹配）
     * @param status running / finished / all
     * @param pageNum 1-based
     * @param pageSize 默认 20，最大 200
     * @return [list, totalCount]
     */
    public Map<String, Object> searchInstances(String defKey, String businessKey,
                                               String actorUserId, String status,
                                               int pageNum, int pageSize)
    {
        int safePageNum = Math.max(1, pageNum);
        int safePageSize = Math.min(200, Math.max(1, pageSize));
        int firstResult = (safePageNum - 1) * safePageSize;
        String s = (status == null || status.isBlank()) ? "all" : status.trim().toLowerCase();

        long total;
        List<ProcessInstanceView> rows;

        if ("running".equals(s))
        {
            var q = runtimeService.createProcessInstanceQuery().active();
            if (defKey != null && !defKey.isBlank()) q = q.processDefinitionKey(defKey);
            if (businessKey != null && !businessKey.isBlank()) q = q.processInstanceBusinessKey(businessKey);
            if (actorUserId != null && !actorUserId.isBlank()) q = q.startedBy(actorUserId);
            total = q.count();
            List<ProcessInstance> list = q.orderByStartTime().desc().listPage(firstResult, safePageSize);
            rows = list.stream().map(this::toView).toList();
        }
        else
        {
            var q = historyService.createHistoricProcessInstanceQuery();
            if (defKey != null && !defKey.isBlank()) q = q.processDefinitionKey(defKey);
            if (businessKey != null && !businessKey.isBlank()) q = q.processInstanceBusinessKey(businessKey);
            if (actorUserId != null && !actorUserId.isBlank()) q = q.startedBy(actorUserId);
            if ("finished".equals(s)) q = q.finished();
            total = q.count();
            List<HistoricProcessInstance> list = q
                    .orderByProcessInstanceStartTime().desc()
                    .listPage(firstResult, safePageSize);
            rows = list.stream().map(this::toView).toList();
        }

        Map<String, Object> ret = new HashMap<>();
        ret.put("rows", rows);
        ret.put("total", total);
        return ret;
    }

    private ProcessInstanceView toView(HistoricProcessInstance hpi)
    {
        ProcessInstanceView v = new ProcessInstanceView();
        v.setId(hpi.getId());
        v.setProcessDefinitionId(hpi.getProcessDefinitionId());
        v.setProcessDefinitionKey(hpi.getProcessDefinitionKey());
        v.setProcessDefinitionName(hpi.getProcessDefinitionName());
        v.setBusinessKey(hpi.getBusinessKey());
        v.setStartUserId(hpi.getStartUserId());
        v.setStartTime(hpi.getStartTime());
        v.setEndTime(hpi.getEndTime());
        v.setEnded(hpi.getEndTime() != null);
        return v;
    }

    public List<HistoricProcessInstance> listMyHistoricInstances(String userId)
    {
        if (userId == null) return List.of();
        return historyService.createHistoricProcessInstanceQuery()
                .startedBy(userId)
                .orderByProcessInstanceStartTime().desc()
                .list();
    }

    @Transactional
    public void cancelInstance(String processInstanceId, String reason)
    {
        runtimeService.deleteProcessInstance(processInstanceId, reason);
    }

    /**
     * 拉取流程实例的运行时态：active / completed / rejected 三组 activity id。
     * 设计目标：让前端 BPMN 流程图可以"已通过节点变绿、当前节点蓝色脉冲、被退回节点橙红"。
     * <ul>
     *   <li>active：未结束实例从 RuntimeService.activeActivityIds 拿；已结束实例为空</li>
     *   <li>completed：取历史 activity 中 endTime != null 的 activityId（去重，且排除 sequenceFlow，
     *       否则前端把箭头本身也染色，画面会过满）</li>
     *   <li>rejected：从流程变量 {@code VAR_REJECTED_ACTIVITY_IDS} 中读取（由退回 API 维护）</li>
     * </ul>
     */
    public ProcessRuntimeStateView getRuntimeState(String processInstanceId)
    {
        if (processInstanceId == null || processInstanceId.isBlank())
        {
            throw new ServiceException("processInstanceId 不能为空");
        }
        HistoricProcessInstance hist = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId).singleResult();
        if (hist == null)
        {
            throw new ServiceException("流程实例不存在: " + processInstanceId);
        }

        ProcessRuntimeStateView v = new ProcessRuntimeStateView();
        v.setProcessInstanceId(processInstanceId);
        v.setProcessDefinitionId(hist.getProcessDefinitionId());
        v.setStartTime(hist.getStartTime());
        v.setEndTime(hist.getEndTime());
        v.setEnded(hist.getEndTime() != null);

        // active activities：只对未结束实例有意义
        List<String> active = new ArrayList<>();
        if (!v.isEnded())
        {
            ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId).singleResult();
            if (pi != null)
            {
                List<Execution> executions = runtimeService.createExecutionQuery()
                        .processInstanceId(processInstanceId).list();
                Set<String> activeSet = new LinkedHashSet<>();
                for (Execution e : executions)
                {
                    if (e.getActivityId() != null) activeSet.add(e.getActivityId());
                }
                active.addAll(activeSet);
            }
        }
        v.setActiveActivityIds(active);

        // completed activities：历史 endTime != null & 非 sequenceFlow
        List<HistoricActivityInstance> all = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId).finished().list();
        Set<String> completed = new LinkedHashSet<>();
        for (HistoricActivityInstance a : all)
        {
            if (a.getActivityId() == null) continue;
            if ("sequenceFlow".equals(a.getActivityType())) continue;
            // active 节点不应再算 completed（防止它两个都亮）
            if (active.contains(a.getActivityId())) continue;
            completed.add(a.getActivityId());
        }
        v.setCompletedActivityIds(new ArrayList<>(completed));

        // rejected：从流程变量取
        Object rejectedVar = null;
        try
        {
            if (!v.isEnded())
            {
                rejectedVar = runtimeService.getVariable(processInstanceId, VAR_REJECTED_ACTIVITY_IDS);
            }
            if (rejectedVar == null)
            {
                rejectedVar = historyService.createHistoricVariableInstanceQuery()
                        .processInstanceId(processInstanceId)
                        .variableName(VAR_REJECTED_ACTIVITY_IDS)
                        .singleResult() != null
                        ? historyService.createHistoricVariableInstanceQuery()
                                .processInstanceId(processInstanceId)
                                .variableName(VAR_REJECTED_ACTIVITY_IDS)
                                .singleResult().getValue()
                        : null;
            }
        }
        catch (Exception ignore)
        {
            // 变量不存在或读取失败时返回空列表，不阻断
        }
        List<String> rejected = Collections.emptyList();
        if (rejectedVar instanceof Collection<?> col)
        {
            rejected = new ArrayList<>();
            for (Object o : col) if (o != null) rejected.add(o.toString());
        }
        v.setRejectedActivityIds(rejected);

        return v;
    }

    // -------------- Timeline --------------

    /**
     * 拉取流程实例的"操作时间轴"：把以下事件统一归一为 {@link TimelineEntry} 列表，按时间升序：
     * <ol>
     *   <li>流程启动 / 结束</li>
     *   <li>历史活动开始 / 结束（仅 userTask、startEvent、endEvent；忽略 sequenceFlow / 网关）</li>
     *   <li>抄送（来自 {@link #VAR_CC_HISTORY}）</li>
     *   <li>后加签（来自 {@link #VAR_ADDSIGN_HISTORY}）</li>
     *   <li>前加签（来自 {@link #VAR_ADDSIGN_BEFORE_HISTORY}）</li>
     *   <li>退回 / 任务评论（来自 Flowable 历史评论；以 "[退回]" 前缀区分）</li>
     * </ol>
     * 设计目标：让前端 ElTimeline 一次拉、一次画，不用做多源拼装。
     */
    public List<TimelineEntry> getInstanceTimeline(String processInstanceId)
    {
        if (processInstanceId == null || processInstanceId.isBlank())
        {
            throw new ServiceException("processInstanceId 不能为空");
        }
        HistoricProcessInstance hist = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId).singleResult();
        if (hist == null)
        {
            throw new ServiceException("流程实例不存在: " + processInstanceId);
        }

        List<TimelineEntry> entries = new ArrayList<>();

        // 1. 流程启动
        if (hist.getStartTime() != null)
        {
            TimelineEntry e = new TimelineEntry(TimelineEntry.Type.PROCESS_START,
                    hist.getStartTime(), "流程启动");
            e.setActor(hist.getStartUserId());
            e.put("processDefinitionId", hist.getProcessDefinitionId());
            e.put("businessKey", hist.getBusinessKey());
            entries.add(e);
        }

        // 2. 历史活动（节点）
        List<HistoricActivityInstance> acts = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime().asc()
                .list();
        for (HistoricActivityInstance a : acts)
        {
            String t = a.getActivityType();
            if (t == null) continue;
            // 仅保留对用户语义清晰的节点；sequenceFlow / 网关线条忽略
            if ("sequenceFlow".equals(t) || "exclusiveGateway".equals(t)
                    || "parallelGateway".equals(t) || "inclusiveGateway".equals(t)) continue;

            if (a.getStartTime() != null)
            {
                TimelineEntry s = new TimelineEntry(TimelineEntry.Type.ACTIVITY_START,
                        a.getStartTime(),
                        nodeLabel(a) + " 到达");
                s.setActor(a.getAssignee());
                s.setActivityId(a.getActivityId());
                s.setTaskId(a.getTaskId());
                s.put("activityType", t);
                s.put("activityName", a.getActivityName());
                entries.add(s);
            }
            if (a.getEndTime() != null)
            {
                TimelineEntry.Type endType = "userTask".equals(t)
                        ? TimelineEntry.Type.TASK_COMPLETE
                        : TimelineEntry.Type.ACTIVITY_END;
                TimelineEntry s = new TimelineEntry(endType,
                        a.getEndTime(),
                        nodeLabel(a) + ("userTask".equals(t) ? " 已完成" : " 结束"));
                s.setActor(a.getAssignee());
                s.setActivityId(a.getActivityId());
                s.setTaskId(a.getTaskId());
                s.put("activityType", t);
                s.put("activityName", a.getActivityName());
                if (a.getDurationInMillis() != null)
                {
                    s.put("durationMs", a.getDurationInMillis());
                }
                entries.add(s);
            }
        }

        // 3. 流程结束
        if (hist.getEndTime() != null)
        {
            TimelineEntry e = new TimelineEntry(TimelineEntry.Type.PROCESS_END,
                    hist.getEndTime(), "流程结束");
            entries.add(e);
        }

        // 4. 抄送
        appendVariableEvents(processInstanceId, VAR_CC_HISTORY, entries,
                (entry, m) ->
                {
                    entry.setType(TimelineEntry.Type.TASK_CC);
                    entry.setActor(strOf(m.get("fromUserId")));
                    entry.setTaskId(strOf(m.get("taskId")));
                    String to = strOf(m.get("toUserId"));
                    String c = strOf(m.get("comment"));
                    entry.setMessage("抄送 → " + to + (c != null ? " :: " + c : ""));
                    entry.put("toUserId", to);
                    entry.put("comment", c);
                });

        // 5. 后加签
        appendVariableEvents(processInstanceId, VAR_ADDSIGN_HISTORY, entries,
                (entry, m) ->
                {
                    entry.setType(TimelineEntry.Type.TASK_ADDSIGN_AFTER);
                    entry.setActor(strOf(m.get("operatorUserId")));
                    entry.setTaskId(strOf(m.get("originTaskId")));
                    String add = strOf(m.get("addedAssignee"));
                    String c = strOf(m.get("comment"));
                    entry.setMessage("后加签 → " + add + (c != null ? " :: " + c : ""));
                    entry.put("addedAssignee", add);
                    entry.put("comment", c);
                });

        // 6. 前加签
        appendVariableEvents(processInstanceId, VAR_ADDSIGN_BEFORE_HISTORY, entries,
                (entry, m) ->
                {
                    entry.setType(TimelineEntry.Type.TASK_ADDSIGN_BEFORE);
                    entry.setActor(strOf(m.get("operatorUserId")));
                    entry.setTaskId(strOf(m.get("originTaskId")));
                    String add = strOf(m.get("addedAssignee"));
                    String child = strOf(m.get("childTaskId"));
                    String c = strOf(m.get("comment"));
                    entry.setMessage("前加签 → " + add
                            + (child != null ? "（任务 " + child + "）" : "")
                            + (c != null ? " :: " + c : ""));
                    entry.put("addedAssignee", add);
                    entry.put("childTaskId", child);
                    entry.put("comment", c);
                    // 撤销标记：cancel-presign 后会在历史项里写 cancelled=true / cancelledBy / cancelledAt
                    Object cancelled = m.get("cancelled");
                    if (cancelled != null) entry.put("cancelled", cancelled);
                    Object cancelledBy = m.get("cancelledBy");
                    if (cancelledBy != null) entry.put("cancelledBy", cancelledBy);
                    Object cancelledAt = m.get("cancelledAt");
                    if (cancelledAt != null) entry.put("cancelledAt", cancelledAt);
                });

        // 7. 评论 / 退回
        try
        {
            List<Comment> comments = taskService.getProcessInstanceComments(processInstanceId);
            if (comments != null) for (Comment c : comments)
            {
                String full = c.getFullMessage();
                if (full == null) full = c.getType();
                if (full == null) full = "";
                Date when = c.getTime();
                if (when == null) continue;
                TimelineEntry e;
                if (full.startsWith("[退回]"))
                {
                    e = new TimelineEntry(TimelineEntry.Type.TASK_SENDBACK, when, full);
                }
                else
                {
                    e = new TimelineEntry(TimelineEntry.Type.TASK_COMMENT, when, full);
                }
                e.setActor(c.getUserId());
                e.setTaskId(c.getTaskId());
                entries.add(e);
            }
        }
        catch (Exception ignore)
        {
            // 老 Flowable 版本可能 deprecated comment，忽略
        }

        // 排序：按 occurredAt 升序，同时间则按 type 优先级稳定
        entries.sort((a, b) ->
        {
            Date da = a.getOccurredAt();
            Date db = b.getOccurredAt();
            if (da == null && db == null) return 0;
            if (da == null) return -1;
            if (db == null) return 1;
            int c = da.compareTo(db);
            if (c != 0) return c;
            // 同时间：让"开始"早于"结束"早于"流程结束"
            return Integer.compare(typeOrder(a.getType()), typeOrder(b.getType()));
        });
        return entries;
    }

    private static int typeOrder(TimelineEntry.Type t)
    {
        if (t == null) return 99;
        return switch (t)
        {
            case PROCESS_START -> 0;
            case ACTIVITY_START -> 1;
            case TASK_COMMENT, TASK_CC, TASK_ADDSIGN_BEFORE, TASK_ADDSIGN_AFTER, TASK_SENDBACK -> 2;
            case TASK_COMPLETE, ACTIVITY_END -> 3;
            case PROCESS_END -> 9;
        };
    }

    private static String nodeLabel(HistoricActivityInstance a)
    {
        if (a.getActivityName() != null && !a.getActivityName().isBlank()) return a.getActivityName();
        return a.getActivityId();
    }

    private static String strOf(Object o)
    {
        if (o == null) return null;
        String s = o.toString();
        return s.isBlank() ? null : s;
    }

    /**
     * 把流程变量里以 List&lt;Map&gt; 结构存的事件转成 TimelineEntry 列表。
     * 由 mapper 负责把 Map 字段填到 entry。
     */
    @SuppressWarnings("unchecked")
    private void appendVariableEvents(String processInstanceId, String varName,
                                      List<TimelineEntry> sink,
                                      java.util.function.BiConsumer<TimelineEntry, Map<String, Object>> mapper)
    {
        Object var = readProcessOrHistoricVariable(processInstanceId, varName);
        if (!(var instanceof List<?>)) return;
        for (Object item : (List<?>) var)
        {
            if (!(item instanceof Map<?, ?> m)) continue;
            Map<String, Object> raw = (Map<String, Object>) m;
            Date when = parseDate(raw.get("occurredAt"));
            TimelineEntry e = new TimelineEntry();
            e.setOccurredAt(when);
            mapper.accept(e, raw);
            sink.add(e);
        }
    }

    private Object readProcessOrHistoricVariable(String processInstanceId, String varName)
    {
        try
        {
            // 流程仍 active 时 runtime 有
            ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId).singleResult();
            if (pi != null) return runtimeService.getVariable(processInstanceId, varName);
        }
        catch (Exception ignore) { /* fallthrough */ }
        try
        {
            var v = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .variableName(varName)
                    .singleResult();
            return v == null ? null : v.getValue();
        }
        catch (Exception ignore)
        {
            return null;
        }
    }

    /** 把流程变量里取出的 occurredAt 字段还原成 Date——可能是 Date 也可能是 timestamp。 */
    private static Date parseDate(Object o)
    {
        if (o == null) return null;
        if (o instanceof Date d) return d;
        if (o instanceof Number n) return new Date(n.longValue());
        try { return new Date(Long.parseLong(o.toString())); }
        catch (Exception ignore) { return null; }
    }

    // -------------- Task --------------

    public List<TaskView> listTodoTasks(String assignee, String keyword)
    {
        if (assignee == null) return List.of();
        var query = taskService.createTaskQuery().taskAssignee(assignee).active();
        if (keyword != null && !keyword.isBlank())
        {
            query = query.taskNameLike("%" + keyword + "%");
        }
        List<Task> tasks = query.orderByTaskCreateTime().desc().list();
        return tasks.stream().map(this::toView).toList();
    }

    /**
     * 列出指定流程实例下所有"未完成"的 task。<br>
     * 用途：
     * <ul>
     *   <li>跨模块联动（M-4 cms-workflow 桥）按 piid 找审核 task 直接 claim + complete</li>
     *   <li>E2E 脚本 / 自动化测试需要按业务 id 反查任务时</li>
     *   <li>未来"撤回 / 加签"按钮在 ProcessProgressDialog 里需要按 piid 列举活跃任务</li>
     * </ul>
     */
    public List<TaskView> listActiveTasksByProcessInstance(String processInstanceId)
    {
        if (processInstanceId == null || processInstanceId.isBlank()) return List.of();
        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .active()
                .orderByTaskCreateTime().asc()
                .list();
        return tasks.stream().map(this::toView).toList();
    }

    public List<TaskView> listDoneTasks(String assignee)
    {
        if (assignee == null) return List.of();
        List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(assignee).finished()
                .orderByHistoricTaskInstanceEndTime().desc()
                .list();
        return tasks.stream().map(this::toView).toList();
    }

    @Transactional
    public void completeTask(String taskId, CompleteTaskRequest req, String userId)
    {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) throw new ServiceException("任务不存在: " + taskId);

        // 前加签阻塞校验：原任务被前加签创建的子任务标记 → 在所有子任务完成前不允许提交
        assertNotBlockedByPreSign(task);

        // 完成前先取 origin id（complete 后 task 会被删，再读不到 local var）
        String preSignOriginTaskId = readPreSignOriginTaskId(task.getId());

        if (req != null && req.getComment() != null && !req.getComment().isBlank())
        {
            taskService.addComment(taskId, task.getProcessInstanceId(), req.getComment());
        }
        if (userId != null) taskService.setAssignee(taskId, userId);
        Map<String, Object> vars = mergeVariables(req);
        if (vars == null || vars.isEmpty()) taskService.complete(taskId);
        else
        {
            if (task.getProcessInstanceId() != null)
            {
                runtimeService.setVariables(task.getProcessInstanceId(), vars);
            }
            taskService.complete(taskId, vars);
        }

        // 子任务完成时唤醒原任务（清除其 scaffoldBlockedByTaskIds 列表中的本任务 id）
        if (preSignOriginTaskId != null)
        {
            unblockOrigin(preSignOriginTaskId, task.getId());
        }
    }

    private String readPreSignOriginProcessInstanceId(String taskId)
    {
        try
        {
            Object v = taskService.getVariableLocal(taskId, VAR_PRESIGN_ORIGIN_PROCESS_INSTANCE_ID);
            return v == null ? null : v.toString();
        }
        catch (Exception ignore)
        {
            return null;
        }
    }

    private String readPreSignOriginTaskId(String taskId)
    {
        try
        {
            Object v = taskService.getVariableLocal(taskId, VAR_PRESIGN_ORIGIN_TASK_ID);
            return v == null ? null : v.toString();
        }
        catch (Exception ignore) { return null; }
    }

    /**
     * 合并用户提交的 variables 与 formData。
     * <ul>
     *   <li>两者都为 null/空时返回 null（让上层走单参 complete，避免无谓 RPC）；</li>
     *   <li>同名 key 由 variables 覆盖 formData——业务侧若需"强制覆盖表单"可显式传 variables。</li>
     * </ul>
     */
    static Map<String, Object> mergeVariables(CompleteTaskRequest req)
    {
        if (req == null) return null;
        Map<String, Object> form = req.getFormData();
        Map<String, Object> sys = req.getVariables();
        boolean formEmpty = form == null || form.isEmpty();
        boolean sysEmpty = sys == null || sys.isEmpty();
        if (formEmpty && sysEmpty) return null;
        if (formEmpty) return new HashMap<>(sys);
        if (sysEmpty) return new HashMap<>(form);
        Map<String, Object> merged = new HashMap<>(form);
        merged.putAll(sys);
        return merged;
    }

    @Transactional
    public void claimTask(String taskId, String userId)
    {
        taskService.claim(taskId, userId);
    }

    @Transactional
    public void unclaimTask(String taskId)
    {
        taskService.unclaim(taskId);
    }

    @Transactional
    public void delegateTask(String taskId, String userId)
    {
        taskService.delegateTask(taskId, userId);
    }

    // -------------- 抄送 / 加签 / 退回 --------------

    /**
     * 抄送：把当前 task 进展告知一组用户，**不创建 task 节点、不阻塞流程**。
     * 实现：
     *   1. 写入流程变量 {@link #VAR_CC_HISTORY}，便于审计与前端时间轴展示；
     *   2. 通过 push bus 给每个接收方发站内信（接入 inbox 模块即落库 message_inbox）。
     */
    @Transactional
    public void cc(String taskId, CcRequest req, String fromUserId)
    {
        if (req == null || req.getReceiverUserIds() == null || req.getReceiverUserIds().isEmpty())
        {
            throw new ServiceException("receiverUserIds 不能为空");
        }
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) throw new ServiceException("任务不存在: " + taskId);

        Date now = req.getOccurredAt() == null ? new Date() : req.getOccurredAt();
        // 1. 追加流程变量
        appendCcHistory(task.getProcessInstanceId(),
                fromUserId, req.getReceiverUserIds(), req.getComment(), now, taskId);

        // 2. 推送（缺失推送总线时静默跳过，不阻断主流程）
        MessagePublisher publisher = messagePublisherProvider.getIfAvailable();
        if (publisher == null)
        {
            log.debug("推送总线不可用，cc 仅落变量 task={} receivers={}", taskId, req.getReceiverUserIds());
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("taskId", task.getId());
        payload.put("taskName", task.getName());
        payload.put("processInstanceId", task.getProcessInstanceId());
        payload.put("processDefinitionId", task.getProcessDefinitionId());
        payload.put("fromUserId", fromUserId);
        payload.put("comment", req.getComment());
        for (String to : req.getReceiverUserIds())
        {
            if (to == null || to.isBlank()) continue;
            try
            {
                publisher.toUser(to, PUSH_TYPE_CC, payload);
            }
            catch (Exception ex)
            {
                log.warn("cc 推送失败 to={} task={} reason={}", to, taskId, ex.getMessage());
            }
        }
    }

    /**
     * 后加签：在当前 task 完成后，给指定用户再开一个相同节点的任务。
     * 实现细节：
     *   - 当前 task 的 owner 设为发起人，assignee 设为加签目标；流程图节点不变；
     *   - 同步追加流程变量 {@link #VAR_ADDSIGN_HISTORY}，便于前端展示加签链路；
     *   - 由调用方决定是否再调用 {@link #completeTask}：本方法本身只创建一个新任务，
     *     原任务保留为待办，避免引擎跳节点。
     */
    @Transactional
    public void addSignAfter(String taskId, AddSignRequest req, String operatorUserId)
    {
        if (req == null || req.getAssignee() == null || req.getAssignee().isBlank())
        {
            throw new ServiceException("加签目标 assignee 不能为空");
        }
        Task origin = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (origin == null) throw new ServiceException("任务不存在: " + taskId);

        // 1. 创建新任务，挂同一个 processInstance & 同一个 taskDefinitionKey（沿用原节点定义），
        //    便于流程图叠色继续把这条 task 算作当前节点。
        Task added = taskService.createTaskBuilder()
                .name(origin.getName() + "（加签）")
                .description(req.getComment())
                .assignee(req.getAssignee())
                .priority(origin.getPriority())
                .create();
        // 关联到流程实例（脱离 BPMN 主流程定义，不会影响后续 sequenceFlow）
        taskService.setVariable(added.getId(), "scaffoldAddSignFromTaskId", taskId);

        // 2. 追加流程变量
        appendAddSignHistory(origin.getProcessInstanceId(),
                taskId, req.getAssignee(), req.getComment(), new Date(), operatorUserId);

        // 3. 推送
        MessagePublisher publisher = messagePublisherProvider.getIfAvailable();
        if (publisher != null)
        {
            try
            {
                publisher.toUser(req.getAssignee(), "workflow.task.addsign", Map.of(
                        "taskId", added.getId(),
                        "taskName", added.getName(),
                        "originTaskId", taskId,
                        "comment", req.getComment() == null ? "" : req.getComment(),
                        "fromUserId", operatorUserId == null ? "" : operatorUserId));
            }
            catch (Exception ex)
            {
                log.warn("addSign 推送失败 to={} reason={}", req.getAssignee(), ex.getMessage());
            }
        }
    }

    /**
     * 前加签：在当前 task 之前并行插入一个新审批人，原任务被阻塞直到加签人完成。
     * 实现细节：
     *   - 创建一个挂在原 processInstance 下的"独立任务"（不在 BPMN sequenceFlow 上）；
     *     新任务携带 {@link #VAR_PRESIGN_ORIGIN_TASK_ID} 局部变量回指原任务，便于完成时主动唤醒；
     *   - 原任务追加 {@link #VAR_BLOCKED_BY_TASK_IDS} 列表，{@link #completeTask} 启动时校验阻塞；
     *   - 流程变量 {@link #VAR_ADDSIGN_BEFORE_HISTORY} 记录历史，便于时间轴 / 审计；
     *   - 推送总线给加签人发"前加签"站内信。
     */
    @Transactional
    public void addSignBefore(String taskId, AddSignBeforeRequest req, String operatorUserId)
    {
        if (req == null || req.getAssignee() == null || req.getAssignee().isBlank())
        {
            throw new ServiceException("前加签目标 assignee 不能为空");
        }
        Task origin = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (origin == null) throw new ServiceException("任务不存在: " + taskId);
        if (req.getAssignee().equals(origin.getAssignee()))
        {
            throw new ServiceException("不能给自己前加签");
        }

        // 1. 创建子任务：独立 task，不进 BPMN sequenceFlow（避免污染流程图）。
        //    Flowable 的 TaskBuilder 不支持直接挂 processInstanceId（standalone task 的设计），
        //    我们退而求其次：在子任务的 local 变量上记录原任务的 processInstanceId，
        //    cancelPreSign / 时间轴回写流程变量时按这个 piid 寻址。
        Task child = taskService.createTaskBuilder()
                .name(origin.getName() + "（前加签）")
                .description(req.getComment())
                .assignee(req.getAssignee())
                .priority(origin.getPriority())
                .create();
        // 2. 子任务回指原任务（local 变量：origin task id + origin process instance id）
        taskService.setVariableLocal(child.getId(), VAR_PRESIGN_ORIGIN_TASK_ID, taskId);
        if (origin.getProcessInstanceId() != null)
        {
            taskService.setVariableLocal(child.getId(),
                    VAR_PRESIGN_ORIGIN_PROCESS_INSTANCE_ID, origin.getProcessInstanceId());
        }

        // 3. 原任务挂阻塞标记（追加，不覆盖；多次前加签会累积）
        appendBlockedByTaskId(taskId, child.getId());

        // 4. 历史变量
        appendAddSignBeforeHistory(origin.getProcessInstanceId(),
                taskId, child.getId(), req.getAssignee(), req.getComment(), new Date(), operatorUserId);

        // 5. 推送
        MessagePublisher publisher = messagePublisherProvider.getIfAvailable();
        if (publisher != null)
        {
            try
            {
                publisher.toUser(req.getAssignee(), PUSH_TYPE_ADDSIGN_BEFORE, Map.of(
                        "taskId", child.getId(),
                        "taskName", child.getName(),
                        "originTaskId", taskId,
                        "comment", req.getComment() == null ? "" : req.getComment(),
                        "fromUserId", operatorUserId == null ? "" : operatorUserId));
            }
            catch (Exception ex)
            {
                log.warn("addSignBefore 推送失败 to={} reason={}", req.getAssignee(), ex.getMessage());
            }
        }
    }

    /**
     * 撤销前加签：在前加签子任务尚未完成前主动撤回。撤销后：
     * <ul>
     *   <li>子任务被 {@code taskService.deleteTask} 删除（不算"已办"）</li>
     *   <li>父任务的 {@link #VAR_BLOCKED_BY_TASK_IDS} 列表里抠掉本子任务 id；列表清空则解除阻塞</li>
     *   <li>流程变量 {@link #VAR_ADDSIGN_BEFORE_HISTORY} 对应记录追加 {@code cancelled=true / cancelledBy / cancelledAt}，
     *       便于时间轴展示"已撤销"</li>
     * </ul>
     * 仅本子任务的发起人（前加签 operator）或 admin 可撤销；其他场景由调用方
     * （Controller @PreAuthorize + 这里的 operator 校验）共同保证。
     *
     * @param childTaskId 前加签创建的子任务 id（不是原任务 id）
     * @param operatorUserId 当前操作者；admin 可绕过 operator 校验
     * @param admin 当前操作者是否管理员（绕过 operator 校验）
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public void cancelPreSign(String childTaskId, String operatorUserId, boolean admin)
    {
        Task child = taskService.createTaskQuery().taskId(childTaskId).singleResult();
        if (child == null) throw new ServiceException("前加签子任务不存在或已结束: " + childTaskId);

        String originTaskId = readPreSignOriginTaskId(childTaskId);
        if (originTaskId == null)
        {
            throw new ServiceException("该任务不是前加签子任务，无法撤销");
        }

        // 找出对应历史记录中的 operator，用于权限校验 & 时间轴展示
        // standalone task 的 processInstanceId 是 null —— 我们在 addSignBefore 阶段用 local 变量记录，
        // 这里依次按 child.local → origin task → origin historic task 三级兜底。
        String processInstanceId = readPreSignOriginProcessInstanceId(childTaskId);
        if (processInstanceId == null)
        {
            Task origin = taskService.createTaskQuery().taskId(originTaskId).singleResult();
            if (origin != null) processInstanceId = origin.getProcessInstanceId();
        }
        if (processInstanceId == null)
        {
            HistoricTaskInstance ho = historyService.createHistoricTaskInstanceQuery()
                    .taskId(originTaskId).singleResult();
            if (ho != null) processInstanceId = ho.getProcessInstanceId();
        }
        Map<String, Object> presignRecord = findPreSignHistoryRecord(processInstanceId, childTaskId);
        String origOperator = presignRecord == null ? null : strOf(presignRecord.get("operatorUserId"));
        if (!admin && origOperator != null && operatorUserId != null
                && !origOperator.equals(operatorUserId))
        {
            throw new ServiceException("仅前加签发起人或管理员可撤销 (operator=" + origOperator + ")");
        }

        // 1. 删除子任务（不会进 ACT_HI_TASKINST 已办；deleteReason 用作 history 备注）
        try
        {
            taskService.deleteTask(childTaskId, "前加签撤销 by " + (operatorUserId == null ? "?" : operatorUserId));
        }
        catch (Exception ex)
        {
            log.warn("撤销前加签 deleteTask 失败 child={} reason={}", childTaskId, ex.getMessage());
            throw new ServiceException("删除子任务失败: " + ex.getMessage());
        }

        // 2. 从父任务的阻塞列表里摘掉
        unblockOrigin(originTaskId, childTaskId);

        // 3. 历史变量打"已撤销"标记，便于时间轴
        markPreSignHistoryCancelled(processInstanceId, childTaskId, operatorUserId);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findPreSignHistoryRecord(String processInstanceId, String childTaskId)
    {
        Object var = readProcessOrHistoricVariable(processInstanceId, VAR_ADDSIGN_BEFORE_HISTORY);
        if (!(var instanceof List<?>)) return null;
        for (Object item : (List<?>) var)
        {
            if (item instanceof Map<?, ?> m && childTaskId.equals(m.get("childTaskId")))
            {
                return (Map<String, Object>) m;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private void markPreSignHistoryCancelled(String processInstanceId, String childTaskId, String operatorUserId)
    {
        if (processInstanceId == null) return;
        try
        {
            ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId).singleResult();
            if (pi == null) return;
            Object cur = runtimeService.getVariable(processInstanceId, VAR_ADDSIGN_BEFORE_HISTORY);
            if (!(cur instanceof List)) return;
            List<Map<String, Object>> hist = new ArrayList<>((List<Map<String, Object>>) cur);
            for (Map<String, Object> m : hist)
            {
                if (childTaskId.equals(m.get("childTaskId")))
                {
                    m.put("cancelled", Boolean.TRUE);
                    m.put("cancelledBy", operatorUserId);
                    m.put("cancelledAt", new Date());
                }
            }
            runtimeService.setVariable(processInstanceId, VAR_ADDSIGN_BEFORE_HISTORY, hist);
        }
        catch (Exception ex)
        {
            log.warn("标记前加签撤销失败 processInstanceId={} child={} reason={}",
                    processInstanceId, childTaskId, ex.getMessage());
        }
    }

    /**
     * 退回：把当前 task 跳转到目标节点（默认上一个 userTask），原节点 id 进 rejected 列表。
     * 用 Flowable 的 changeActivityStateBuilder 实现。
     */
    @Transactional
    public void sendBack(String taskId, SendBackRequest req, String operatorUserId)
    {
        if (req == null || req.getComment() == null || req.getComment().isBlank())
        {
            throw new ServiceException("退回必须填写理由 comment");
        }
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) throw new ServiceException("任务不存在: " + taskId);

        String currentActivityId = task.getTaskDefinitionKey();
        String targetActivityId = req.getTargetActivityId();
        if (targetActivityId == null || targetActivityId.isBlank())
        {
            targetActivityId = findLastUserTaskActivityId(task.getProcessInstanceId(), currentActivityId);
            if (targetActivityId == null)
            {
                throw new ServiceException("找不到可退回的上一个 userTask；请显式指定 targetActivityId");
            }
        }

        // 评论存到当前 task，方便审计
        taskService.addComment(taskId, task.getProcessInstanceId(),
                "[退回] -> " + targetActivityId + " :: " + req.getComment());

        // 把 currentActivityId 标记为 rejected
        appendRejectedActivity(task.getProcessInstanceId(), currentActivityId);

        // 跳转
        if (operatorUserId != null) Authentication.setAuthenticatedUserId(operatorUserId);
        try
        {
            runtimeService.createChangeActivityStateBuilder()
                    .processInstanceId(task.getProcessInstanceId())
                    .moveActivityIdTo(currentActivityId, targetActivityId)
                    .changeState();
        }
        finally
        {
            if (operatorUserId != null) Authentication.setAuthenticatedUserId(null);
        }
    }

    /** 历史中，沿当前 active activity 往前回溯，找最近一个已结束的 userTask 的 activityId。 */
    @SuppressWarnings("unchecked")
    private String findLastUserTaskActivityId(String processInstanceId, String currentActivityId)
    {
        List<HistoricActivityInstance> finished = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .activityType("userTask")
                .finished()
                .orderByHistoricActivityInstanceEndTime().desc()
                .list();
        for (HistoricActivityInstance a : finished)
        {
            if (currentActivityId != null && currentActivityId.equals(a.getActivityId())) continue;
            return a.getActivityId();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private void appendCcHistory(String processInstanceId, String fromUserId,
                                 List<String> receivers, String comment, Date occurredAt, String taskId)
    {
        try
        {
            Object cur = runtimeService.getVariable(processInstanceId, VAR_CC_HISTORY);
            List<Map<String, Object>> hist = (cur instanceof List)
                    ? new ArrayList<>((List<Map<String, Object>>) cur)
                    : new ArrayList<>();
            for (String to : receivers)
            {
                Map<String, Object> entry = new HashMap<>();
                entry.put("fromUserId", fromUserId);
                entry.put("toUserId", to);
                entry.put("comment", comment);
                entry.put("occurredAt", occurredAt);
                entry.put("taskId", taskId);
                hist.add(entry);
            }
            runtimeService.setVariable(processInstanceId, VAR_CC_HISTORY, hist);
        }
        catch (Exception ex)
        {
            log.warn("写 cc history 失败 processInstanceId={} reason={}", processInstanceId, ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void appendAddSignHistory(String processInstanceId, String originTaskId,
                                      String addedAssignee, String comment, Date occurredAt,
                                      String operatorUserId)
    {
        try
        {
            Object cur = runtimeService.getVariable(processInstanceId, VAR_ADDSIGN_HISTORY);
            List<Map<String, Object>> hist = (cur instanceof List)
                    ? new ArrayList<>((List<Map<String, Object>>) cur)
                    : new ArrayList<>();
            Map<String, Object> entry = new HashMap<>();
            entry.put("originTaskId", originTaskId);
            entry.put("addedAssignee", addedAssignee);
            entry.put("comment", comment);
            entry.put("occurredAt", occurredAt);
            entry.put("operatorUserId", operatorUserId);
            hist.add(entry);
            runtimeService.setVariable(processInstanceId, VAR_ADDSIGN_HISTORY, hist);
        }
        catch (Exception ex)
        {
            log.warn("写 add-sign history 失败 reason={}", ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void appendAddSignBeforeHistory(String processInstanceId, String originTaskId,
                                            String childTaskId, String addedAssignee,
                                            String comment, Date occurredAt, String operatorUserId)
    {
        try
        {
            Object cur = runtimeService.getVariable(processInstanceId, VAR_ADDSIGN_BEFORE_HISTORY);
            List<Map<String, Object>> hist = (cur instanceof List)
                    ? new ArrayList<>((List<Map<String, Object>>) cur)
                    : new ArrayList<>();
            Map<String, Object> entry = new HashMap<>();
            entry.put("originTaskId", originTaskId);
            entry.put("childTaskId", childTaskId);
            entry.put("addedAssignee", addedAssignee);
            entry.put("comment", comment);
            entry.put("occurredAt", occurredAt);
            entry.put("operatorUserId", operatorUserId);
            hist.add(entry);
            runtimeService.setVariable(processInstanceId, VAR_ADDSIGN_BEFORE_HISTORY, hist);
        }
        catch (Exception ex)
        {
            log.warn("写 add-sign-before history 失败 reason={}", ex.getMessage());
        }
    }

    /**
     * 把 blockingTaskId 追加到原任务的 {@link #VAR_BLOCKED_BY_TASK_IDS} 局部变量中。
     * 用 task local var 而不是 process var：避免一个流程实例中多个并行 task 互相干扰。
     */
    @SuppressWarnings("unchecked")
    private void appendBlockedByTaskId(String originTaskId, String blockingTaskId)
    {
        try
        {
            Object cur = taskService.getVariableLocal(originTaskId, VAR_BLOCKED_BY_TASK_IDS);
            List<String> list = (cur instanceof List)
                    ? new ArrayList<>((List<String>) cur)
                    : new ArrayList<>();
            if (!list.contains(blockingTaskId)) list.add(blockingTaskId);
            taskService.setVariableLocal(originTaskId, VAR_BLOCKED_BY_TASK_IDS, list);
        }
        catch (Exception ex)
        {
            log.warn("写 blocked-by 失败 origin={} blocker={} reason={}",
                    originTaskId, blockingTaskId, ex.getMessage());
        }
    }

    /**
     * 前加签阻塞校验：原任务被打上 {@link #VAR_BLOCKED_BY_TASK_IDS}（task local var）后，
     * 必须等所有 child task 完成才能提交。child task 完成时由
     * {@link #unblockOriginIfThisIsPreSignChild(Task)} 主动清理。
     */
    @SuppressWarnings("unchecked")
    private void assertNotBlockedByPreSign(Task task)
    {
        Object v;
        try { v = taskService.getVariableLocal(task.getId(), VAR_BLOCKED_BY_TASK_IDS); }
        catch (Exception ignore) { return; }
        if (!(v instanceof Collection<?> c) || c.isEmpty()) return;
        // 仍存在未完成的子任务才算阻塞
        List<String> alive = new ArrayList<>();
        for (Object o : c)
        {
            if (o == null) continue;
            String childId = o.toString();
            Task child = taskService.createTaskQuery().taskId(childId).singleResult();
            if (child != null) alive.add(childId);
        }
        if (alive.isEmpty())
        {
            try { taskService.removeVariableLocal(task.getId(), VAR_BLOCKED_BY_TASK_IDS); }
            catch (Exception ignore) { /* 容错 */ }
            return;
        }
        throw new ServiceException("该任务被前加签阻塞，请等待加签人 (taskId=" + alive + ") 完成后再提交");
    }

    /**
     * 子任务完成后，清除原任务 {@link #VAR_BLOCKED_BY_TASK_IDS} 中对应的 id；
     * 列表清空时移除变量本身。
     */
    @SuppressWarnings("unchecked")
    private void unblockOrigin(String originTaskId, String completedChildTaskId)
    {
        Task origin = taskService.createTaskQuery().taskId(originTaskId).singleResult();
        if (origin == null) return; // 原任务已不存在（被退回 / 完成等）

        try
        {
            Object cur = taskService.getVariableLocal(originTaskId, VAR_BLOCKED_BY_TASK_IDS);
            if (cur instanceof Collection<?> col)
            {
                List<String> remaining = new ArrayList<>();
                for (Object o : col)
                {
                    if (o == null) continue;
                    String id = o.toString();
                    if (!id.equals(completedChildTaskId)) remaining.add(id);
                }
                if (remaining.isEmpty())
                {
                    taskService.removeVariableLocal(originTaskId, VAR_BLOCKED_BY_TASK_IDS);
                }
                else
                {
                    taskService.setVariableLocal(originTaskId, VAR_BLOCKED_BY_TASK_IDS, remaining);
                }
            }
        }
        catch (Exception ex)
        {
            log.warn("清除前加签阻塞标记失败 origin={} child={} reason={}",
                    originTaskId, completedChildTaskId, ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void appendRejectedActivity(String processInstanceId, String activityId)
    {
        try
        {
            Object cur = runtimeService.getVariable(processInstanceId, VAR_REJECTED_ACTIVITY_IDS);
            List<String> rejected = (cur instanceof List)
                    ? new ArrayList<>((List<String>) cur)
                    : new ArrayList<>();
            if (!rejected.contains(activityId)) rejected.add(activityId);
            runtimeService.setVariable(processInstanceId, VAR_REJECTED_ACTIVITY_IDS, rejected);
        }
        catch (Exception ex)
        {
            log.warn("写 rejected history 失败 reason={}", ex.getMessage());
        }
    }

    // -------------- Mapping --------------

    private ProcessInstanceView toView(ProcessInstance pi)
    {
        ProcessInstanceView v = new ProcessInstanceView();
        v.setId(pi.getId());
        v.setProcessDefinitionId(pi.getProcessDefinitionId());
        v.setProcessDefinitionKey(pi.getProcessDefinitionKey());
        v.setProcessDefinitionName(pi.getProcessDefinitionName());
        v.setBusinessKey(pi.getBusinessKey());
        v.setStartUserId(pi.getStartUserId());
        v.setStartTime(pi.getStartTime());
        v.setActivityId(pi.getActivityId());
        v.setEnded(pi.isEnded());
        v.setSuspended(pi.isSuspended());
        return v;
    }

    private TaskView toView(Task t)
    {
        TaskView v = new TaskView();
        v.setId(t.getId());
        v.setName(t.getName());
        v.setDescription(t.getDescription());
        v.setAssignee(t.getAssignee());
        v.setOwner(t.getOwner());
        v.setProcessInstanceId(t.getProcessInstanceId());
        v.setProcessDefinitionId(t.getProcessDefinitionId());
        v.setProcessDefinitionKey(extractDefinitionKey(t.getProcessDefinitionId()));
        v.setTaskDefinitionKey(t.getTaskDefinitionKey());
        v.setCreateTime(t.getCreateTime());
        v.setClaimTime(t.getClaimTime());
        v.setDueDate(t.getDueDate());
        v.setPriority(t.getPriority());
        v.setSuspended(t.isSuspended());
        v.setBlockedByTaskIds(readActiveBlockedByTaskIds(t.getId()));
        return v;
    }

    /**
     * 读 task local var {@link #VAR_BLOCKED_BY_TASK_IDS}，过滤掉已不存在（已完成 / 撤销）
     * 的子任务 id；列表为空说明没在被阻塞，前端继续按普通任务渲染。
     */
    @SuppressWarnings("unchecked")
    private List<String> readActiveBlockedByTaskIds(String taskId)
    {
        Object cur;
        try { cur = taskService.getVariableLocal(taskId, VAR_BLOCKED_BY_TASK_IDS); }
        catch (Exception ignore) { return Collections.emptyList(); }
        if (!(cur instanceof Collection<?> col) || col.isEmpty()) return Collections.emptyList();
        List<String> alive = new ArrayList<>();
        for (Object o : col)
        {
            if (o == null) continue;
            String childId = o.toString();
            Task child = taskService.createTaskQuery().taskId(childId).singleResult();
            if (child != null) alive.add(childId);
        }
        return alive;
    }

    private TaskView toView(HistoricTaskInstance t)
    {
        TaskView v = new TaskView();
        v.setId(t.getId());
        v.setName(t.getName());
        v.setDescription(t.getDescription());
        v.setAssignee(t.getAssignee());
        v.setOwner(t.getOwner());
        v.setProcessInstanceId(t.getProcessInstanceId());
        v.setProcessDefinitionId(t.getProcessDefinitionId());
        v.setProcessDefinitionKey(extractDefinitionKey(t.getProcessDefinitionId()));
        v.setTaskDefinitionKey(t.getTaskDefinitionKey());
        v.setCreateTime(t.getCreateTime());
        v.setClaimTime(t.getClaimTime());
        v.setEndTime(t.getEndTime());
        v.setDueDate(t.getDueDate());
        v.setPriority(t.getPriority());
        return v;
    }

    /**
     * Flowable processDefinitionId 形如 {@code leave:1:abc-uuid}，用第一段作为 key；
     * 解析失败时返回 null（前端会降级到任务名 / processDefinitionId 显示）。
     */
    static String extractDefinitionKey(String processDefinitionId)
    {
        if (processDefinitionId == null) return null;
        int idx = processDefinitionId.indexOf(':');
        return idx > 0 ? processDefinitionId.substring(0, idx) : processDefinitionId;
    }
}
