package com.scaffold.module.workflow.listener;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.engine.RuntimeService;
import org.flowable.task.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import com.scaffold.framework.web.websocket.bus.MessagePublisher;
import jakarta.annotation.PostConstruct;
import java.util.Map;

/**
 * 监听 Flowable 任务事件 → 推送站内信。
 * <p>
 * 通过 ObjectProvider 软依赖 MessagePublisher——即使推送总线模块未启用也不会启动失败，
 * 仅打 debug 日志跳过推送。
 *
 * @author scaffold
 */
@Component
public class TaskNotifyEventListener implements FlowableEventListener
{
    private static final Logger log = LoggerFactory.getLogger(TaskNotifyEventListener.class);

    private static final String TYPE_TASK_CREATED = "workflow.task.created";
    private static final String TYPE_TASK_COMPLETED = "workflow.task.completed";

    private final ObjectProvider<MessagePublisher> publisherProvider;
    private final RuntimeService runtimeService;

    public TaskNotifyEventListener(ObjectProvider<MessagePublisher> publisherProvider,
                                   RuntimeService runtimeService)
    {
        this.publisherProvider = publisherProvider;
        this.runtimeService = runtimeService;
    }

    /**
     * 通过 RuntimeService 在引擎启动后注册自己（避免 Flowable 启动期监听不到的窗口期）。
     * Spring 容器就绪后调用即可。
     */
    @PostConstruct
    public void register()
    {
        runtimeService.addEventListener(this,
                FlowableEngineEventType.TASK_CREATED,
                FlowableEngineEventType.TASK_COMPLETED);
        log.info("WorkflowTaskNotifyListener registered for TASK_CREATED / TASK_COMPLETED");
    }

    @Override
    public void onEvent(FlowableEvent event)
    {
        if (!(event instanceof FlowableEntityEvent entityEvent)) return;
        Object entity = entityEvent.getEntity();
        if (!(entity instanceof Task task)) return;

        FlowableEngineEventType type = (FlowableEngineEventType) event.getType();
        MessagePublisher publisher = publisherProvider.getIfAvailable();
        if (publisher == null)
        {
            log.debug("MessagePublisher 不可用，跳过推送 task={} type={}", task.getId(), type);
            return;
        }

        try
        {
            Map<String, Object> payload = Map.of(
                    "taskId", task.getId(),
                    "taskName", task.getName() == null ? "" : task.getName(),
                    "processInstanceId", task.getProcessInstanceId(),
                    "processDefinitionId", task.getProcessDefinitionId());
            switch (type)
            {
                case TASK_CREATED -> {
                    String assignee = task.getAssignee();
                    if (assignee == null || assignee.isBlank()) return;
                    publisher.toUser(assignee, TYPE_TASK_CREATED, payload);
                }
                case TASK_COMPLETED -> {
                    String owner = task.getOwner();
                    if (owner == null || owner.isBlank()) owner = task.getAssignee();
                    if (owner == null) return;
                    publisher.toUser(owner, TYPE_TASK_COMPLETED, payload);
                }
                default -> { /* ignore */ }
            }
        }
        catch (Exception ex)
        {
            log.warn("推送任务事件失败 task={} type={} reason={}", task.getId(), type, ex.getMessage());
        }
    }

    @Override
    public boolean isFailOnException() { return false; }

    @Override
    public boolean isFireOnTransactionLifecycleEvent() { return false; }

    @Override
    public String getOnTransaction() { return null; }
}
