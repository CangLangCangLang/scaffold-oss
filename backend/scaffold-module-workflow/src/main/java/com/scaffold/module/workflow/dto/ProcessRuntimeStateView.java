package com.scaffold.module.workflow.dto;

import java.util.Date;
import java.util.List;

/**
 * 流程实例运行时态：用于前端 BPMN 流程图叠色（已通过 / 当前 / 退回）。
 *
 * @author scaffold
 */
public class ProcessRuntimeStateView
{
    private String processInstanceId;

    /** 当前在执行的 activity id 列表（多实例 / 并行情况下可能多个） */
    private List<String> activeActivityIds;

    /** 历史已完成的 activity id（去重） */
    private List<String> completedActivityIds;

    /** 历史中被退回过的 activity id —— 用于上色"被打回过的节点" */
    private List<String> rejectedActivityIds;

    /** 流程是否已结束 */
    private boolean ended;

    /** 流程定义 id（前端拿它去取 BPMN XML） */
    private String processDefinitionId;

    /** 流程开始时间 */
    private Date startTime;

    /** 流程结束时间，未结束为 null */
    private Date endTime;

    public String getProcessInstanceId() { return processInstanceId; }
    public void setProcessInstanceId(String v) { this.processInstanceId = v; }

    public List<String> getActiveActivityIds() { return activeActivityIds; }
    public void setActiveActivityIds(List<String> v) { this.activeActivityIds = v; }

    public List<String> getCompletedActivityIds() { return completedActivityIds; }
    public void setCompletedActivityIds(List<String> v) { this.completedActivityIds = v; }

    public List<String> getRejectedActivityIds() { return rejectedActivityIds; }
    public void setRejectedActivityIds(List<String> v) { this.rejectedActivityIds = v; }

    public boolean isEnded() { return ended; }
    public void setEnded(boolean v) { this.ended = v; }

    public String getProcessDefinitionId() { return processDefinitionId; }
    public void setProcessDefinitionId(String v) { this.processDefinitionId = v; }

    public Date getStartTime() { return startTime; }
    public void setStartTime(Date v) { this.startTime = v; }

    public Date getEndTime() { return endTime; }
    public void setEndTime(Date v) { this.endTime = v; }
}
