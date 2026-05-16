package com.scaffold.module.workflow.dto;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 流程实例时间轴的一条事件。
 * <p>
 * 设计目标：把 Flowable 的历史活动 + 抄送 / 后加签 / 前加签 / 退回 / 评论 全部归一到同一条 timeline，
 * 由前端 ElTimeline 一致地渲染，不再让 UI 端每次都拼。
 *
 * @author scaffold
 */
public class TimelineEntry
{
    /** 处理类型，前端可据此选图标 / 颜色 */
    public enum Type
    {
        /** 流程实例启动 */
        PROCESS_START("process.start"),
        /** 流程实例结束 */
        PROCESS_END("process.end"),
        /** BPMN 活动节点开始 */
        ACTIVITY_START("activity.start"),
        /** BPMN 活动节点结束 */
        ACTIVITY_END("activity.end"),
        /** Flowable 任务被完成（含审批意见） */
        TASK_COMPLETE("task.complete"),
        /** 抄送（不阻塞流程） */
        TASK_CC("task.cc"),
        /** 后加签：当前任务后插入 */
        TASK_ADDSIGN_AFTER("task.addsign.after"),
        /** 前加签：当前任务前并行插入 */
        TASK_ADDSIGN_BEFORE("task.addsign.before"),
        /** 退回：跳到上一个 userTask */
        TASK_SENDBACK("task.sendback"),
        /** 任务评论 */
        TASK_COMMENT("task.comment");

        private final String code;
        Type(String code) { this.code = code; }
        public String getCode() { return code; }
    }

    private Type type;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    private Date occurredAt;
    private String actor;
    private String activityId;
    private String taskId;
    private String message;
    private Map<String, Object> extra;

    public TimelineEntry() {}

    public TimelineEntry(Type type, Date occurredAt, String message)
    {
        this.type = type;
        this.occurredAt = occurredAt;
        this.message = message;
    }

    public TimelineEntry put(String k, Object v)
    {
        if (this.extra == null) this.extra = new HashMap<>();
        this.extra.put(k, v);
        return this;
    }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    /** 给前端用的字符串形式 type，例如 "task.cc"。 */
    public String getCode() { return type == null ? null : type.getCode(); }

    public Date getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Date occurredAt) { this.occurredAt = occurredAt; }
    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
    public String getActivityId() { return activityId; }
    public void setActivityId(String activityId) { this.activityId = activityId; }
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Map<String, Object> getExtra() { return extra; }
    public void setExtra(Map<String, Object> extra) { this.extra = extra; }
}
