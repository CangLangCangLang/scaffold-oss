package com.scaffold.module.workflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricActivityInstanceQuery;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.runtime.ChangeActivityStateBuilder;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ExecutionQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceBuilder;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.task.api.TaskBuilder;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import com.scaffold.common.exception.ServiceException;
import com.scaffold.framework.web.websocket.bus.MessagePublisher;
import com.scaffold.module.workflow.dto.AddSignBeforeRequest;
import com.scaffold.module.workflow.dto.AddSignRequest;
import com.scaffold.module.workflow.dto.CcRequest;
import com.scaffold.module.workflow.dto.CompleteTaskRequest;
import com.scaffold.module.workflow.dto.SendBackRequest;
import com.scaffold.module.workflow.dto.StartProcessRequest;

/**
 * WorkflowFacade 单测：用 Mockito 替代 Flowable 引擎，仅验证 Facade 的业务逻辑
 * （参数校验、字段映射、调用顺序）。
 */
class WorkflowFacadeTest
{
    private RepositoryService repositoryService;
    private RuntimeService runtimeService;
    private TaskService taskService;
    private HistoryService historyService;
    private MessagePublisher messagePublisher;
    private WorkflowFacade facade;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp()
    {
        repositoryService = mock(RepositoryService.class);
        runtimeService = mock(RuntimeService.class);
        taskService = mock(TaskService.class);
        historyService = mock(HistoryService.class);
        messagePublisher = mock(MessagePublisher.class);

        ObjectProvider<MessagePublisher> publisherProvider = mock(ObjectProvider.class);
        when(publisherProvider.getIfAvailable()).thenReturn(messagePublisher);

        facade = new WorkflowFacade();
        org.springframework.test.util.ReflectionTestUtils.setField(facade, "repositoryService", repositoryService);
        org.springframework.test.util.ReflectionTestUtils.setField(facade, "runtimeService", runtimeService);
        org.springframework.test.util.ReflectionTestUtils.setField(facade, "taskService", taskService);
        org.springframework.test.util.ReflectionTestUtils.setField(facade, "historyService", historyService);
        org.springframework.test.util.ReflectionTestUtils.setField(facade, "messagePublisherProvider", publisherProvider);
    }

    @Test
    void startProcessRejectsBlankKey()
    {
        StartProcessRequest req = new StartProcessRequest();
        assertThatThrownBy(() -> facade.startProcess(req, "1"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("processDefinitionKey");
    }

    @Test
    void startProcessUsesProvidedVariablesAndBusinessKey()
    {
        StartProcessRequest req = new StartProcessRequest();
        req.setProcessDefinitionKey("leave");
        req.setBusinessKey("biz-1");
        Map<String, Object> vars = new HashMap<>();
        vars.put("days", 3);
        req.setVariables(vars);

        ProcessInstance pi = mock(ProcessInstance.class);
        when(pi.getId()).thenReturn("pi-1");
        when(pi.getProcessDefinitionKey()).thenReturn("leave");
        when(pi.getProcessDefinitionId()).thenReturn("leave:1:1");
        when(runtimeService.startProcessInstanceByKey(eq("leave"), eq("biz-1"), anyMap()))
                .thenReturn(pi);

        var view = facade.startProcess(req, "42");
        assertThat(view.getId()).isEqualTo("pi-1");
        assertThat(view.getProcessDefinitionKey()).isEqualTo("leave");
        verify(runtimeService).startProcessInstanceByKey(eq("leave"), eq("biz-1"), anyMap());
        verify(runtimeService, never()).setProcessInstanceName(anyString(), anyString());
    }

    @Test
    void startProcessSetsNameWhenProvided()
    {
        StartProcessRequest req = new StartProcessRequest();
        req.setProcessDefinitionKey("leave");
        req.setName("alice 的请假");

        ProcessInstance pi = mock(ProcessInstance.class);
        when(pi.getId()).thenReturn("pi-2");
        when(runtimeService.startProcessInstanceByKey(eq("leave"), any(), anyMap())).thenReturn(pi);

        facade.startProcess(req, null);
        verify(runtimeService, times(1)).setProcessInstanceName("pi-2", "alice 的请假");
    }

    @Test
    void completeTaskThrowsWhenTaskMissing()
    {
        TaskQuery query = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(query);
        when(query.taskId(anyString())).thenReturn(query);
        when(query.singleResult()).thenReturn(null);

        assertThatThrownBy(() -> facade.completeTask("missing", null, "1"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("任务不存在");
    }

    @Test
    void completeTaskAddsCommentAndCompletesWithVariables()
    {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn("t1");
        when(task.getProcessInstanceId()).thenReturn("pi-1");
        TaskQuery query = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(query);
        when(query.taskId("t1")).thenReturn(query);
        when(query.singleResult()).thenReturn(task);

        CompleteTaskRequest req = new CompleteTaskRequest();
        req.setComment("OK");
        Map<String, Object> vars = Map.of("approve", true);
        req.setVariables(vars);

        facade.completeTask("t1", req, "alice");

        verify(taskService).addComment("t1", "pi-1", "OK");
        verify(taskService).setAssignee("t1", "alice");
        // mergeVariables 会拷贝成 HashMap，比对 entry 而不是同一引用
        verify(taskService).complete(eq("t1"), eq(new HashMap<>(vars)));
    }

    @Test
    void completeTaskMergesFormDataAndVariablesWithSystemPriority()
    {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn("t1");
        when(task.getProcessInstanceId()).thenReturn("pi-1");
        TaskQuery query = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(query);
        when(query.taskId("t1")).thenReturn(query);
        when(query.singleResult()).thenReturn(task);

        CompleteTaskRequest req = new CompleteTaskRequest();
        Map<String, Object> form = new HashMap<>();
        form.put("amount", 100);
        form.put("approve", false); // 与 variables 同名 → 期望被 variables 覆盖
        Map<String, Object> sys = new HashMap<>();
        sys.put("approve", true);
        req.setFormData(form);
        req.setVariables(sys);

        facade.completeTask("t1", req, null);

        Map<String, Object> expected = new HashMap<>();
        expected.put("amount", 100);
        expected.put("approve", true);
        verify(taskService).complete(eq("t1"), eq(expected));
    }

    @Test
    void completeTaskWithOnlyFormDataAlsoCallsTwoArgComplete()
    {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn("t1");
        when(task.getProcessInstanceId()).thenReturn("pi-1");
        TaskQuery query = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(query);
        when(query.taskId("t1")).thenReturn(query);
        when(query.singleResult()).thenReturn(task);

        CompleteTaskRequest req = new CompleteTaskRequest();
        req.setFormData(Map.of("days", 3));

        facade.completeTask("t1", req, null);

        verify(taskService).complete(eq("t1"), eq(new HashMap<>(Map.of("days", 3))));
    }

    @Test
    void mergeVariablesReturnsNullWhenBothEmpty()
    {
        assertThat(WorkflowFacade.mergeVariables(null)).isNull();
        CompleteTaskRequest req = new CompleteTaskRequest();
        assertThat(WorkflowFacade.mergeVariables(req)).isNull();
    }

    @Test
    void extractDefinitionKeyHandlesFlowableIdFormat()
    {
        assertThat(WorkflowFacade.extractDefinitionKey("leave:1:abc-uuid")).isEqualTo("leave");
        assertThat(WorkflowFacade.extractDefinitionKey("simple")).isEqualTo("simple");
        assertThat(WorkflowFacade.extractDefinitionKey(null)).isNull();
    }

    @Test
    void completeTaskWithoutVariablesUsesSingleArgComplete()
    {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn("t1");
        when(task.getProcessInstanceId()).thenReturn("pi-1");
        TaskQuery query = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(query);
        when(query.taskId("t1")).thenReturn(query);
        when(query.singleResult()).thenReturn(task);

        facade.completeTask("t1", null, null);

        verify(taskService).complete("t1");
        verify(taskService, never()).complete(anyString(), anyMap());
    }

    @Test
    void listTodoTasksShortCircuitsOnNullAssignee()
    {
        assertThat(facade.listTodoTasks(null, null)).isEmpty();
        verify(taskService, never()).createTaskQuery();
    }

    @Test
    void listTodoTasksMapsTaskFieldsToView()
    {
        Task t = mock(Task.class);
        when(t.getId()).thenReturn("t1");
        when(t.getName()).thenReturn("approve");
        when(t.getAssignee()).thenReturn("alice");
        when(t.getProcessInstanceId()).thenReturn("pi-1");
        when(t.getCreateTime()).thenReturn(new Date());

        TaskQuery query = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(query);
        when(query.taskAssignee("alice")).thenReturn(query);
        when(query.active()).thenReturn(query);
        when(query.orderByTaskCreateTime()).thenReturn(query);
        when(query.desc()).thenReturn(query);
        when(query.list()).thenReturn(List.of(t));

        var views = facade.listTodoTasks("alice", null);
        assertThat(views).hasSize(1);
        assertThat(views.get(0).getId()).isEqualTo("t1");
        assertThat(views.get(0).getName()).isEqualTo("approve");
        assertThat(views.get(0).getAssignee()).isEqualTo("alice");
    }

    @Test
    void listTodoTasksFillsBlockedByTaskIdsWhenPreSignChildAlive()
    {
        Task t = mock(Task.class);
        when(t.getId()).thenReturn("t1");
        when(t.getName()).thenReturn("approve");
        when(t.getAssignee()).thenReturn("alice");
        when(t.getCreateTime()).thenReturn(new Date());

        TaskQuery query = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(query);
        when(query.taskAssignee("alice")).thenReturn(query);
        when(query.active()).thenReturn(query);
        when(query.orderByTaskCreateTime()).thenReturn(query);
        when(query.desc()).thenReturn(query);
        when(query.list()).thenReturn(List.of(t));

        // toView(t) 内部 readActiveBlockedByTaskIds → 父 task local var 列表里 t-child / t-stale
        when(taskService.getVariableLocal("t1", WorkflowFacade.VAR_BLOCKED_BY_TASK_IDS))
                .thenReturn(new java.util.ArrayList<>(List.of("t-child", "t-stale")));
        // 子任务存活性校验：t-child 仍在；t-stale 已被删（query.singleResult() 返回 null）
        when(query.taskId(anyString())).thenReturn(query);
        Task aliveChild = mock(Task.class);
        when(aliveChild.getId()).thenReturn("t-child");
        when(query.singleResult()).thenReturn(aliveChild, (Task) null);

        var views = facade.listTodoTasks("alice", null);
        assertThat(views).hasSize(1);
        assertThat(views.get(0).getBlockedByTaskIds()).containsExactly("t-child");
    }

    @Test
    void listTodoTasksLeavesBlockedByTaskIdsEmptyWhenNoVar()
    {
        Task t = mock(Task.class);
        when(t.getId()).thenReturn("t1");
        TaskQuery query = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(query);
        when(query.taskAssignee(anyString())).thenReturn(query);
        when(query.active()).thenReturn(query);
        when(query.orderByTaskCreateTime()).thenReturn(query);
        when(query.desc()).thenReturn(query);
        when(query.list()).thenReturn(List.of(t));
        when(taskService.getVariableLocal("t1", WorkflowFacade.VAR_BLOCKED_BY_TASK_IDS))
                .thenReturn(null);

        var views = facade.listTodoTasks("alice", null);
        assertThat(views.get(0).getBlockedByTaskIds()).isEmpty();
    }

    @Test
    void claimAndUnclaimDelegateToTaskService()
    {
        facade.claimTask("t1", "alice");
        verify(taskService).claim("t1", "alice");

        facade.unclaimTask("t2");
        verify(taskService).unclaim("t2");

        facade.delegateTask("t3", "bob");
        verify(taskService).delegateTask("t3", "bob");
    }

    @Test
    void cancelInstanceDelegatesToRuntimeService()
    {
        facade.cancelInstance("pi-1", "user_cancel");
        verify(runtimeService).deleteProcessInstance("pi-1", "user_cancel");
    }

    @Test
    void getRuntimeStateRejectsBlankId()
    {
        assertThatThrownBy(() -> facade.getRuntimeState(""))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("processInstanceId");
    }

    @Test
    void getRuntimeStateForRunningInstanceCollectsActiveAndCompleted()
    {
        HistoricProcessInstance hist = mock(HistoricProcessInstance.class);
        when(hist.getEndTime()).thenReturn(null); // 未结束
        when(hist.getProcessDefinitionId()).thenReturn("def:1:1");
        HistoricProcessInstanceQuery hpq = mock(HistoricProcessInstanceQuery.class);
        when(historyService.createHistoricProcessInstanceQuery()).thenReturn(hpq);
        when(hpq.processInstanceId("pi-1")).thenReturn(hpq);
        when(hpq.singleResult()).thenReturn(hist);

        ProcessInstance pi = mock(ProcessInstance.class);
        ProcessInstanceQuery piq = mock(ProcessInstanceQuery.class);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(piq);
        when(piq.processInstanceId("pi-1")).thenReturn(piq);
        when(piq.singleResult()).thenReturn(pi);

        Execution e1 = mock(Execution.class);
        when(e1.getActivityId()).thenReturn("Task_1");
        Execution e2 = mock(Execution.class);
        when(e2.getActivityId()).thenReturn(null); // 主实例本身没 activity
        ExecutionQuery eq = mock(ExecutionQuery.class);
        when(runtimeService.createExecutionQuery()).thenReturn(eq);
        when(eq.processInstanceId("pi-1")).thenReturn(eq);
        when(eq.list()).thenReturn(List.of(e1, e2));

        HistoricActivityInstance a1 = mock(HistoricActivityInstance.class);
        when(a1.getActivityId()).thenReturn("StartEvent_1");
        when(a1.getActivityType()).thenReturn("startEvent");
        HistoricActivityInstance a2 = mock(HistoricActivityInstance.class);
        when(a2.getActivityId()).thenReturn("Flow_1");
        when(a2.getActivityType()).thenReturn("sequenceFlow"); // 应被过滤
        HistoricActivityInstance a3 = mock(HistoricActivityInstance.class);
        when(a3.getActivityId()).thenReturn("Task_1");           // 当前 active，不应进 completed
        when(a3.getActivityType()).thenReturn("userTask");
        HistoricActivityInstanceQuery haq = mock(HistoricActivityInstanceQuery.class);
        when(historyService.createHistoricActivityInstanceQuery()).thenReturn(haq);
        when(haq.processInstanceId("pi-1")).thenReturn(haq);
        when(haq.finished()).thenReturn(haq);
        when(haq.list()).thenReturn(List.of(a1, a2, a3));

        when(runtimeService.getVariable("pi-1", WorkflowFacade.VAR_REJECTED_ACTIVITY_IDS))
                .thenReturn(List.of("Task_1"));

        var view = facade.getRuntimeState("pi-1");
        assertThat(view.isEnded()).isFalse();
        assertThat(view.getProcessDefinitionId()).isEqualTo("def:1:1");
        assertThat(view.getActiveActivityIds()).containsExactly("Task_1");
        assertThat(view.getCompletedActivityIds()).containsExactly("StartEvent_1");
        assertThat(view.getRejectedActivityIds()).containsExactly("Task_1");
    }

    @Test
    void getRuntimeStateThrowsWhenInstanceMissing()
    {
        HistoricProcessInstanceQuery hpq = mock(HistoricProcessInstanceQuery.class);
        when(historyService.createHistoricProcessInstanceQuery()).thenReturn(hpq);
        when(hpq.processInstanceId(anyString())).thenReturn(hpq);
        when(hpq.singleResult()).thenReturn(null);

        assertThatThrownBy(() -> facade.getRuntimeState("nope"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("流程实例不存在");
    }

    @Test
    void ccRejectsEmptyReceivers()
    {
        assertThatThrownBy(() -> facade.cc("t1", new CcRequest(), "1"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("receiverUserIds");
    }

    @Test
    void ccPushesToEachReceiverAndAppendsHistory()
    {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn("t1");
        when(task.getName()).thenReturn("approve");
        when(task.getProcessInstanceId()).thenReturn("pi-1");
        TaskQuery query = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(query);
        when(query.taskId("t1")).thenReturn(query);
        when(query.singleResult()).thenReturn(task);

        when(runtimeService.getVariable("pi-1", WorkflowFacade.VAR_CC_HISTORY)).thenReturn(null);

        CcRequest req = new CcRequest();
        req.setReceiverUserIds(List.of("alice", "bob"));
        req.setComment("FYI");

        facade.cc("t1", req, "operator");

        verify(messagePublisher).toUser(eq("alice"), eq(WorkflowFacade.PUSH_TYPE_CC), any());
        verify(messagePublisher).toUser(eq("bob"), eq(WorkflowFacade.PUSH_TYPE_CC), any());
        verify(runtimeService).setVariable(eq("pi-1"), eq(WorkflowFacade.VAR_CC_HISTORY), any());
    }

    @Test
    void addSignAfterCreatesNewTaskAndAppendsHistory()
    {
        Task origin = mock(Task.class);
        when(origin.getId()).thenReturn("t1");
        when(origin.getName()).thenReturn("approve");
        when(origin.getPriority()).thenReturn(50);
        when(origin.getProcessInstanceId()).thenReturn("pi-1");
        TaskQuery q = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(q);
        when(q.taskId("t1")).thenReturn(q);
        when(q.singleResult()).thenReturn(origin);

        TaskBuilder builder = mock(TaskBuilder.class, org.mockito.Mockito.RETURNS_SELF);
        when(taskService.createTaskBuilder()).thenReturn(builder);
        Task added = mock(Task.class);
        when(added.getId()).thenReturn("t2");
        when(added.getName()).thenReturn("approve（加签）");
        when(builder.create()).thenReturn(added);

        AddSignRequest req = new AddSignRequest();
        req.setAssignee("charlie");
        req.setComment("协助审批");

        facade.addSignAfter("t1", req, "operator");

        verify(builder).assignee("charlie");
        verify(builder).create();
        verify(taskService).setVariable("t2", "scaffoldAddSignFromTaskId", "t1");
        verify(runtimeService).setVariable(eq("pi-1"), eq(WorkflowFacade.VAR_ADDSIGN_HISTORY), any());
        verify(messagePublisher).toUser(eq("charlie"), eq("workflow.task.addsign"), any());
    }

    @Test
    void sendBackRejectsMissingComment()
    {
        SendBackRequest req = new SendBackRequest();
        assertThatThrownBy(() -> facade.sendBack("t1", req, "1"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("理由");
    }

    @Test
    void sendBackUsesProvidedTargetAndAppendsRejected()
    {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn("t1");
        when(task.getTaskDefinitionKey()).thenReturn("Task_2");
        when(task.getProcessInstanceId()).thenReturn("pi-1");
        TaskQuery q = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(q);
        when(q.taskId("t1")).thenReturn(q);
        when(q.singleResult()).thenReturn(task);

        ChangeActivityStateBuilder builder = mock(ChangeActivityStateBuilder.class, org.mockito.Mockito.RETURNS_SELF);
        when(runtimeService.createChangeActivityStateBuilder()).thenReturn(builder);
        when(runtimeService.getVariable("pi-1", WorkflowFacade.VAR_REJECTED_ACTIVITY_IDS)).thenReturn(null);

        SendBackRequest req = new SendBackRequest();
        req.setTargetActivityId("Task_1");
        req.setComment("信息不完整");

        facade.sendBack("t1", req, "operator");

        verify(taskService).addComment(eq("t1"), eq("pi-1"), org.mockito.ArgumentMatchers.contains("Task_1"));
        verify(builder).processInstanceId("pi-1");
        verify(builder).moveActivityIdTo("Task_2", "Task_1");
        verify(builder).changeState();
        verify(runtimeService).setVariable(eq("pi-1"), eq(WorkflowFacade.VAR_REJECTED_ACTIVITY_IDS), any());
    }

    @Test
    void sendBackFallsBackToLastUserTaskWhenTargetMissing()
    {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn("t1");
        when(task.getTaskDefinitionKey()).thenReturn("Task_2");
        when(task.getProcessInstanceId()).thenReturn("pi-1");
        TaskQuery q = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(q);
        when(q.taskId("t1")).thenReturn(q);
        when(q.singleResult()).thenReturn(task);

        HistoricActivityInstance prev = mock(HistoricActivityInstance.class);
        when(prev.getActivityId()).thenReturn("Task_1");
        HistoricActivityInstanceQuery haq = mock(HistoricActivityInstanceQuery.class);
        when(historyService.createHistoricActivityInstanceQuery()).thenReturn(haq);
        when(haq.processInstanceId("pi-1")).thenReturn(haq);
        when(haq.activityType("userTask")).thenReturn(haq);
        when(haq.finished()).thenReturn(haq);
        when(haq.orderByHistoricActivityInstanceEndTime()).thenReturn(haq);
        when(haq.desc()).thenReturn(haq);
        when(haq.list()).thenReturn(List.of(prev));

        ChangeActivityStateBuilder builder = mock(ChangeActivityStateBuilder.class, org.mockito.Mockito.RETURNS_SELF);
        when(runtimeService.createChangeActivityStateBuilder()).thenReturn(builder);

        SendBackRequest req = new SendBackRequest();
        req.setComment("退回上一节点");

        facade.sendBack("t1", req, "operator");

        verify(builder).moveActivityIdTo("Task_2", "Task_1");
    }

    @Test
    void addSignBeforeRejectsBlankAssignee()
    {
        AddSignBeforeRequest req = new AddSignBeforeRequest();
        assertThatThrownBy(() -> facade.addSignBefore("t1", req, "1"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("assignee");
    }

    @Test
    void addSignBeforeRejectsSelfAssignee()
    {
        Task origin = mock(Task.class);
        when(origin.getId()).thenReturn("t1");
        when(origin.getAssignee()).thenReturn("alice");
        TaskQuery q = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(q);
        when(q.taskId("t1")).thenReturn(q);
        when(q.singleResult()).thenReturn(origin);

        AddSignBeforeRequest req = new AddSignBeforeRequest();
        req.setAssignee("alice");

        assertThatThrownBy(() -> facade.addSignBefore("t1", req, "operator"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("自己");
    }

    @Test
    void addSignBeforeCreatesChildTaskAndBlocksOrigin()
    {
        Task origin = mock(Task.class);
        when(origin.getId()).thenReturn("t1");
        when(origin.getName()).thenReturn("approve");
        when(origin.getAssignee()).thenReturn("alice");
        when(origin.getPriority()).thenReturn(50);
        when(origin.getProcessInstanceId()).thenReturn("pi-1");
        TaskQuery q = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(q);
        when(q.taskId("t1")).thenReturn(q);
        when(q.singleResult()).thenReturn(origin);

        TaskBuilder builder = mock(TaskBuilder.class, org.mockito.Mockito.RETURNS_SELF);
        when(taskService.createTaskBuilder()).thenReturn(builder);
        Task child = mock(Task.class);
        when(child.getId()).thenReturn("t-child");
        when(child.getName()).thenReturn("approve（前加签）");
        when(builder.create()).thenReturn(child);

        AddSignBeforeRequest req = new AddSignBeforeRequest();
        req.setAssignee("charlie");
        req.setComment("协助审批");

        facade.addSignBefore("t1", req, "operator");

        verify(builder).assignee("charlie");
        verify(builder).create();
        verify(taskService).setVariableLocal("t-child",
                WorkflowFacade.VAR_PRESIGN_ORIGIN_TASK_ID, "t1");
        verify(taskService).setVariableLocal(eq("t1"),
                eq(WorkflowFacade.VAR_BLOCKED_BY_TASK_IDS), any());
        verify(runtimeService).setVariable(eq("pi-1"),
                eq(WorkflowFacade.VAR_ADDSIGN_BEFORE_HISTORY), any());
        verify(messagePublisher).toUser(eq("charlie"),
                eq(WorkflowFacade.PUSH_TYPE_ADDSIGN_BEFORE), any());
    }

    @Test
    void completeTaskBlockedByPreSignThrows()
    {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn("t1");
        when(task.getProcessInstanceId()).thenReturn("pi-1");
        TaskQuery query = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(query);
        when(query.taskId("t1")).thenReturn(query);
        when(query.singleResult()).thenReturn(task);

        // 原任务被前加签子任务 t-child 阻塞，且子任务仍存在
        when(taskService.getVariableLocal("t1", WorkflowFacade.VAR_BLOCKED_BY_TASK_IDS))
                .thenReturn(new java.util.ArrayList<>(List.of("t-child")));
        TaskQuery childQ = mock(TaskQuery.class);
        // 这里复用同一个 mock 也行；为简洁让 createTaskQuery 第二次返回独立 mock
        Task child = mock(Task.class);
        when(child.getId()).thenReturn("t-child");
        // 因为 createTaskQuery() 在 Mockito 默认会按调用顺序返回；这里我们用同一个 query stub 覆盖
        when(query.taskId("t-child")).thenReturn(query);
        // singleResult 已被前面 stub 成 task；为本次重新 stub
        when(query.singleResult()).thenReturn(task, child);

        assertThatThrownBy(() -> facade.completeTask("t1", null, null))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("阻塞");

        verify(taskService, never()).complete(anyString());
    }

    @Test
    void completeTaskClearsPreSignBlockOnChildCompletion()
    {
        Task child = mock(Task.class);
        when(child.getId()).thenReturn("t-child");
        when(child.getProcessInstanceId()).thenReturn("pi-1");

        Task origin = mock(Task.class);
        when(origin.getId()).thenReturn("t1");

        TaskQuery query = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(query);
        when(query.taskId(anyString())).thenReturn(query);
        // 调用顺序：completeTask 先 query t-child（=child）；然后 unblock 段 query t1（=origin）
        when(query.singleResult()).thenReturn(child, origin);

        // 前加签子任务回指 origin
        when(taskService.getVariableLocal("t-child", WorkflowFacade.VAR_PRESIGN_ORIGIN_TASK_ID))
                .thenReturn("t1");
        // origin 当前 blocked_by 列表里只有 t-child，完成后应被清空
        when(taskService.getVariableLocal("t1", WorkflowFacade.VAR_BLOCKED_BY_TASK_IDS))
                .thenReturn(new java.util.ArrayList<>(List.of("t-child")));

        facade.completeTask("t-child", null, null);

        verify(taskService).complete("t-child");
        verify(taskService).removeVariableLocal("t1", WorkflowFacade.VAR_BLOCKED_BY_TASK_IDS);
    }

    @Test
    void getInstanceTimelineRejectsBlankId()
    {
        assertThatThrownBy(() -> facade.getInstanceTimeline(""))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("processInstanceId");
    }

    @Test
    void getInstanceTimelineMergesActivitiesAndCcHistory()
    {
        // 流程实例已结束（这样才会去查 historic var）
        HistoricProcessInstance hist = mock(HistoricProcessInstance.class);
        Date startTime = new Date(1_700_000_000_000L);
        Date endTime = new Date(1_700_000_900_000L);
        when(hist.getStartTime()).thenReturn(startTime);
        when(hist.getEndTime()).thenReturn(endTime);
        when(hist.getStartUserId()).thenReturn("alice");
        when(hist.getProcessDefinitionId()).thenReturn("def:1:1");
        HistoricProcessInstanceQuery hpq = mock(HistoricProcessInstanceQuery.class);
        when(historyService.createHistoricProcessInstanceQuery()).thenReturn(hpq);
        when(hpq.processInstanceId("pi-1")).thenReturn(hpq);
        when(hpq.singleResult()).thenReturn(hist);

        // 一个 userTask 节点，开始 + 结束
        HistoricActivityInstance act = mock(HistoricActivityInstance.class);
        when(act.getActivityType()).thenReturn("userTask");
        when(act.getActivityId()).thenReturn("Task_1");
        when(act.getActivityName()).thenReturn("Approve");
        when(act.getStartTime()).thenReturn(new Date(1_700_000_100_000L));
        when(act.getEndTime()).thenReturn(new Date(1_700_000_500_000L));
        when(act.getDurationInMillis()).thenReturn(400_000L);
        HistoricActivityInstanceQuery haq = mock(HistoricActivityInstanceQuery.class);
        when(historyService.createHistoricActivityInstanceQuery()).thenReturn(haq);
        when(haq.processInstanceId("pi-1")).thenReturn(haq);
        when(haq.orderByHistoricActivityInstanceStartTime()).thenReturn(haq);
        when(haq.asc()).thenReturn(haq);
        when(haq.list()).thenReturn(List.of(act));

        // 流程已结束 → runtime query 返回 null，从 historic var 读 cc history
        ProcessInstanceQuery piq = mock(ProcessInstanceQuery.class);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(piq);
        when(piq.processInstanceId("pi-1")).thenReturn(piq);
        when(piq.singleResult()).thenReturn(null);

        Map<String, Object> ccEntry = new HashMap<>();
        ccEntry.put("fromUserId", "alice");
        ccEntry.put("toUserId", "bob");
        ccEntry.put("comment", "FYI");
        ccEntry.put("occurredAt", new Date(1_700_000_200_000L));
        ccEntry.put("taskId", "T-CC");

        @SuppressWarnings("rawtypes")
        org.flowable.variable.api.history.HistoricVariableInstanceQuery vq =
                mock(org.flowable.variable.api.history.HistoricVariableInstanceQuery.class);
        org.flowable.variable.api.history.HistoricVariableInstance vi =
                mock(org.flowable.variable.api.history.HistoricVariableInstance.class);
        when(historyService.createHistoricVariableInstanceQuery()).thenReturn(vq);
        when(vq.processInstanceId("pi-1")).thenReturn(vq);
        when(vq.variableName(anyString())).thenReturn(vq);
        when(vq.singleResult()).thenReturn(vi);
        when(vi.getValue()).thenReturn(List.of(ccEntry), List.of(), List.of());

        // 评论列表为空（默认 mock）
        when(taskService.getProcessInstanceComments("pi-1")).thenReturn(List.of());

        var timeline = facade.getInstanceTimeline("pi-1");

        assertThat(timeline).isNotEmpty();
        assertThat(timeline)
                .extracting(com.scaffold.module.workflow.dto.TimelineEntry::getType)
                .contains(
                        com.scaffold.module.workflow.dto.TimelineEntry.Type.PROCESS_START,
                        com.scaffold.module.workflow.dto.TimelineEntry.Type.ACTIVITY_START,
                        com.scaffold.module.workflow.dto.TimelineEntry.Type.TASK_CC,
                        com.scaffold.module.workflow.dto.TimelineEntry.Type.TASK_COMPLETE,
                        com.scaffold.module.workflow.dto.TimelineEntry.Type.PROCESS_END);
        // 严格升序
        Date prev = null;
        for (var e : timeline)
        {
            if (prev != null) assertThat(e.getOccurredAt()).isAfterOrEqualTo(prev);
            prev = e.getOccurredAt();
        }
    }

    @Test
    void cancelPreSignRejectsWhenNotPreSignChild()
    {
        Task t = mock(Task.class);
        when(t.getId()).thenReturn("t-other");
        TaskQuery q = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(q);
        when(q.taskId("t-other")).thenReturn(q);
        when(q.singleResult()).thenReturn(t);
        when(taskService.getVariableLocal("t-other", WorkflowFacade.VAR_PRESIGN_ORIGIN_TASK_ID))
                .thenReturn(null);

        assertThatThrownBy(() -> facade.cancelPreSign("t-other", "operator", false))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("不是前加签子任务");
    }

    @Test
    void cancelPreSignRejectsNonOriginatorWhenNotAdmin()
    {
        Task child = mock(Task.class);
        when(child.getId()).thenReturn("t-child");
        when(child.getProcessInstanceId()).thenReturn("pi-1");
        TaskQuery q = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(q);
        when(q.taskId("t-child")).thenReturn(q);
        when(q.singleResult()).thenReturn(child);
        when(taskService.getVariableLocal("t-child", WorkflowFacade.VAR_PRESIGN_ORIGIN_TASK_ID))
                .thenReturn("t1");

        // history 中记录 operator=alice
        Map<String, Object> rec = new HashMap<>();
        rec.put("childTaskId", "t-child");
        rec.put("operatorUserId", "alice");
        ProcessInstance pi = mock(ProcessInstance.class);
        ProcessInstanceQuery piq = mock(ProcessInstanceQuery.class);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(piq);
        when(piq.processInstanceId("pi-1")).thenReturn(piq);
        when(piq.singleResult()).thenReturn(pi);
        when(runtimeService.getVariable("pi-1", WorkflowFacade.VAR_ADDSIGN_BEFORE_HISTORY))
                .thenReturn(new java.util.ArrayList<>(List.of(rec)));

        assertThatThrownBy(() -> facade.cancelPreSign("t-child", "bob", false))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("发起人或管理员");
    }

    @Test
    void cancelPreSignDeletesChildAndUnblocksOrigin()
    {
        Task child = mock(Task.class);
        when(child.getId()).thenReturn("t-child");
        when(child.getProcessInstanceId()).thenReturn("pi-1");

        Task origin = mock(Task.class);
        when(origin.getId()).thenReturn("t1");

        TaskQuery q = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(q);
        when(q.taskId(anyString())).thenReturn(q);
        // 三次 singleResult：1) 校验子任务存在 2) unblockOrigin 时拿 origin
        when(q.singleResult()).thenReturn(child, origin);

        when(taskService.getVariableLocal("t-child", WorkflowFacade.VAR_PRESIGN_ORIGIN_TASK_ID))
                .thenReturn("t1");

        // history 由发起人 alice 创建，operator 跟着也是 alice → 校验通过
        Map<String, Object> rec = new HashMap<>();
        rec.put("childTaskId", "t-child");
        rec.put("operatorUserId", "alice");
        java.util.List<Map<String, Object>> hist = new java.util.ArrayList<>(List.of(rec));
        ProcessInstance pi = mock(ProcessInstance.class);
        ProcessInstanceQuery piq = mock(ProcessInstanceQuery.class);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(piq);
        when(piq.processInstanceId("pi-1")).thenReturn(piq);
        when(piq.singleResult()).thenReturn(pi);
        when(runtimeService.getVariable("pi-1", WorkflowFacade.VAR_ADDSIGN_BEFORE_HISTORY))
                .thenReturn(hist);

        // origin 仅被这一个子任务阻塞
        when(taskService.getVariableLocal("t1", WorkflowFacade.VAR_BLOCKED_BY_TASK_IDS))
                .thenReturn(new java.util.ArrayList<>(List.of("t-child")));

        facade.cancelPreSign("t-child", "alice", false);

        verify(taskService).deleteTask(eq("t-child"), anyString());
        verify(taskService).removeVariableLocal("t1", WorkflowFacade.VAR_BLOCKED_BY_TASK_IDS);
        // history 被打了 cancelled 标
        verify(runtimeService).setVariable(eq("pi-1"),
                eq(WorkflowFacade.VAR_ADDSIGN_BEFORE_HISTORY), any());
        assertThat(rec.get("cancelled")).isEqualTo(Boolean.TRUE);
    }

    @Test
    void cancelPreSignAllowsAdminBypassOriginatorCheck()
    {
        Task child = mock(Task.class);
        when(child.getId()).thenReturn("t-child");
        when(child.getProcessInstanceId()).thenReturn("pi-1");
        Task origin = mock(Task.class);
        when(origin.getId()).thenReturn("t1");

        TaskQuery q = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(q);
        when(q.taskId(anyString())).thenReturn(q);
        when(q.singleResult()).thenReturn(child, origin);

        when(taskService.getVariableLocal("t-child", WorkflowFacade.VAR_PRESIGN_ORIGIN_TASK_ID))
                .thenReturn("t1");
        Map<String, Object> rec = new HashMap<>();
        rec.put("childTaskId", "t-child");
        rec.put("operatorUserId", "alice");
        ProcessInstance pi = mock(ProcessInstance.class);
        ProcessInstanceQuery piq = mock(ProcessInstanceQuery.class);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(piq);
        when(piq.processInstanceId("pi-1")).thenReturn(piq);
        when(piq.singleResult()).thenReturn(pi);
        when(runtimeService.getVariable("pi-1", WorkflowFacade.VAR_ADDSIGN_BEFORE_HISTORY))
                .thenReturn(new java.util.ArrayList<>(List.of(rec)));
        when(taskService.getVariableLocal("t1", WorkflowFacade.VAR_BLOCKED_BY_TASK_IDS))
                .thenReturn(new java.util.ArrayList<>(List.of("t-child")));

        // admin=true 即便 operator=bob 也不抛
        facade.cancelPreSign("t-child", "bob", true);
        verify(taskService).deleteTask(eq("t-child"), anyString());
    }

    @Test
    void searchInstancesRunningQueriesRuntime()
    {
        ProcessInstanceQuery piq = mock(ProcessInstanceQuery.class);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(piq);
        when(piq.active()).thenReturn(piq);
        when(piq.processDefinitionKey(anyString())).thenReturn(piq);
        when(piq.processInstanceBusinessKey(anyString())).thenReturn(piq);
        when(piq.startedBy(anyString())).thenReturn(piq);
        when(piq.orderByStartTime()).thenReturn(piq);
        when(piq.desc()).thenReturn(piq);

        ProcessInstance pi = mock(ProcessInstance.class);
        when(pi.getId()).thenReturn("pi-1");
        when(piq.count()).thenReturn(5L);
        when(piq.listPage(eq(0), eq(20))).thenReturn(List.of(pi));

        Map<String, Object> ret = facade.searchInstances("leave", "B-1", "alice", "running", 1, 20);
        assertThat(ret.get("total")).isEqualTo(5L);
        assertThat((List<?>) ret.get("rows")).hasSize(1);
        verify(piq).processDefinitionKey("leave");
        verify(piq).processInstanceBusinessKey("B-1");
        verify(piq).startedBy("alice");
    }

    @Test
    void searchInstancesFinishedQueriesHistoryWithFinishedFilter()
    {
        HistoricProcessInstanceQuery hpq = mock(HistoricProcessInstanceQuery.class);
        when(historyService.createHistoricProcessInstanceQuery()).thenReturn(hpq);
        when(hpq.processDefinitionKey(anyString())).thenReturn(hpq);
        when(hpq.processInstanceBusinessKey(anyString())).thenReturn(hpq);
        when(hpq.startedBy(anyString())).thenReturn(hpq);
        when(hpq.finished()).thenReturn(hpq);
        when(hpq.orderByProcessInstanceStartTime()).thenReturn(hpq);
        when(hpq.desc()).thenReturn(hpq);
        when(hpq.count()).thenReturn(2L);

        HistoricProcessInstance hp = mock(HistoricProcessInstance.class);
        when(hp.getId()).thenReturn("pi-h");
        when(hp.getEndTime()).thenReturn(new Date());
        when(hpq.listPage(eq(20), eq(20))).thenReturn(List.of(hp));

        Map<String, Object> ret = facade.searchInstances(null, null, null, "finished", 2, 20);
        assertThat(ret.get("total")).isEqualTo(2L);
        verify(hpq).finished();
    }

    @Test
    void searchInstancesAllStatusUsesHistoryWithoutFinishedFilter()
    {
        HistoricProcessInstanceQuery hpq = mock(HistoricProcessInstanceQuery.class);
        when(historyService.createHistoricProcessInstanceQuery()).thenReturn(hpq);
        when(hpq.orderByProcessInstanceStartTime()).thenReturn(hpq);
        when(hpq.desc()).thenReturn(hpq);
        when(hpq.count()).thenReturn(0L);
        when(hpq.listPage(eq(0), eq(20))).thenReturn(List.of());

        facade.searchInstances(null, null, null, "all", 1, 20);
        verify(hpq, never()).finished();
    }

    @Test
    void listVersionsByKeyRejectsBlankKey()
    {
        assertThatThrownBy(() -> facade.listVersionsByKey(null))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("processDefinitionKey");
        assertThatThrownBy(() -> facade.listVersionsByKey("  "))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("processDefinitionKey");
    }

    @Test
    void listVersionsByKeyReturnsEmptyWhenNoMatches()
    {
        org.flowable.engine.repository.ProcessDefinitionQuery pdq =
                mock(org.flowable.engine.repository.ProcessDefinitionQuery.class);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(pdq);
        when(pdq.processDefinitionKey(anyString())).thenReturn(pdq);
        when(pdq.orderByProcessDefinitionVersion()).thenReturn(pdq);
        when(pdq.desc()).thenReturn(pdq);
        when(pdq.list()).thenReturn(List.of());

        var versions = facade.listVersionsByKey("missing_key");
        assertThat(versions).isEmpty();
        verify(pdq).processDefinitionKey("missing_key");
    }

    @Test
    void listVersionsByKeyOrderedDescAndMapsAllFields()
    {
        org.flowable.engine.repository.ProcessDefinition v3 =
                mock(org.flowable.engine.repository.ProcessDefinition.class);
        when(v3.getId()).thenReturn("k:3:c");
        when(v3.getKey()).thenReturn("k");
        when(v3.getName()).thenReturn("流程 K");
        when(v3.getVersion()).thenReturn(3);
        when(v3.getDescription()).thenReturn("第三版");
        when(v3.getResourceName()).thenReturn("k.bpmn20.xml");
        when(v3.getDeploymentId()).thenReturn("dep-3");
        when(v3.isSuspended()).thenReturn(false);

        org.flowable.engine.repository.ProcessDefinition v1 =
                mock(org.flowable.engine.repository.ProcessDefinition.class);
        when(v1.getId()).thenReturn("k:1:a");
        when(v1.getKey()).thenReturn("k");
        when(v1.getVersion()).thenReturn(1);
        when(v1.getDeploymentId()).thenReturn("dep-1");
        when(v1.isSuspended()).thenReturn(true);

        org.flowable.engine.repository.ProcessDefinitionQuery pdq =
                mock(org.flowable.engine.repository.ProcessDefinitionQuery.class);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(pdq);
        when(pdq.processDefinitionKey(anyString())).thenReturn(pdq);
        when(pdq.orderByProcessDefinitionVersion()).thenReturn(pdq);
        when(pdq.desc()).thenReturn(pdq);
        when(pdq.list()).thenReturn(List.of(v3, v1));

        // deployment 反查
        org.flowable.engine.repository.DeploymentQuery dq =
                mock(org.flowable.engine.repository.DeploymentQuery.class);
        when(repositoryService.createDeploymentQuery()).thenReturn(dq);
        when(dq.deploymentId(anyString())).thenReturn(dq);
        org.flowable.engine.repository.Deployment dep3 = mock(org.flowable.engine.repository.Deployment.class);
        Date t3 = new Date(2_000_000_000L);
        when(dep3.getDeploymentTime()).thenReturn(t3);
        org.flowable.engine.repository.Deployment dep1 = mock(org.flowable.engine.repository.Deployment.class);
        Date t1 = new Date(1_000_000_000L);
        when(dep1.getDeploymentTime()).thenReturn(t1);
        // 两次 singleResult 按 list 顺序返回
        when(dq.singleResult()).thenReturn(dep3, dep1);

        var views = facade.listVersionsByKey("k");
        assertThat(views).hasSize(2);

        var vv3 = views.get(0);
        assertThat(vv3.getVersion()).isEqualTo(3);
        assertThat(vv3.getId()).isEqualTo("k:3:c");
        assertThat(vv3.getKey()).isEqualTo("k");
        assertThat(vv3.getName()).isEqualTo("流程 K");
        assertThat(vv3.getDescription()).isEqualTo("第三版");
        assertThat(vv3.getResourceName()).isEqualTo("k.bpmn20.xml");
        assertThat(vv3.getDeploymentId()).isEqualTo("dep-3");
        assertThat(vv3.isSuspended()).isFalse();
        assertThat(vv3.getDeploymentTime()).isEqualTo(t3);

        var vv1 = views.get(1);
        assertThat(vv1.getVersion()).isEqualTo(1);
        assertThat(vv1.isSuspended()).isTrue();
        assertThat(vv1.getDeploymentTime()).isEqualTo(t1);
    }

    @Test
    void listVersionsByKeyToleratesMissingDeployment()
    {
        // deployment 查不到时只是 deploymentTime=null，不抛
        org.flowable.engine.repository.ProcessDefinition v1 =
                mock(org.flowable.engine.repository.ProcessDefinition.class);
        when(v1.getVersion()).thenReturn(1);
        when(v1.getDeploymentId()).thenReturn("dep-x");

        org.flowable.engine.repository.ProcessDefinitionQuery pdq =
                mock(org.flowable.engine.repository.ProcessDefinitionQuery.class);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(pdq);
        when(pdq.processDefinitionKey(anyString())).thenReturn(pdq);
        when(pdq.orderByProcessDefinitionVersion()).thenReturn(pdq);
        when(pdq.desc()).thenReturn(pdq);
        when(pdq.list()).thenReturn(List.of(v1));

        org.flowable.engine.repository.DeploymentQuery dq =
                mock(org.flowable.engine.repository.DeploymentQuery.class);
        when(repositoryService.createDeploymentQuery()).thenReturn(dq);
        when(dq.deploymentId(anyString())).thenReturn(dq);
        when(dq.singleResult()).thenReturn(null);

        var views = facade.listVersionsByKey("k");
        assertThat(views).hasSize(1);
        assertThat(views.get(0).getDeploymentTime()).isNull();
    }

    /** 让代码引用 ProcessInstanceBuilder 的目的是确保运行时 classpath 上 Flowable 能被解析（无功能）。 */
    @SuppressWarnings("unused")
    private void touchFlowableSymbols()
    {
        ProcessInstanceBuilder b = null;
    }
}
