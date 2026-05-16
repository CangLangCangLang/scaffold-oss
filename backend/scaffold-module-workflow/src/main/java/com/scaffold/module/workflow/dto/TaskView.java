package com.scaffold.module.workflow.dto;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 任务视图（待办 / 已办通用）。
 *
 * @author scaffold
 */
public class TaskView
{
    private String id;
    private String name;
    private String description;
    private String assignee;
    private String owner;
    private String processInstanceId;
    private String processDefinitionId;
    private String processDefinitionKey;
    private String processDefinitionName;
    private String businessKey;
    private String taskDefinitionKey;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date claimTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date dueDate;
    private Integer priority;
    private boolean suspended;

    /**
     * 当前任务被前加签阻塞的子任务 id 列表（仅在待办列表里有意义；非 null 即提示前端 disable
     * 提交按钮 + 显示 “被加签阻塞” 标签）。
     * <p>
     * 已办列表 / 后端 toView(HistoricTaskInstance) 不会回填本字段。
     */
    private List<String> blockedByTaskIds = Collections.emptyList();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAssignee() { return assignee; }
    public void setAssignee(String assignee) { this.assignee = assignee; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getProcessInstanceId() { return processInstanceId; }
    public void setProcessInstanceId(String s) { this.processInstanceId = s; }
    public String getProcessDefinitionId() { return processDefinitionId; }
    public void setProcessDefinitionId(String s) { this.processDefinitionId = s; }
    public String getProcessDefinitionKey() { return processDefinitionKey; }
    public void setProcessDefinitionKey(String s) { this.processDefinitionKey = s; }
    public String getProcessDefinitionName() { return processDefinitionName; }
    public void setProcessDefinitionName(String s) { this.processDefinitionName = s; }
    public String getBusinessKey() { return businessKey; }
    public void setBusinessKey(String s) { this.businessKey = s; }
    public String getTaskDefinitionKey() { return taskDefinitionKey; }
    public void setTaskDefinitionKey(String s) { this.taskDefinitionKey = s; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date d) { this.createTime = d; }
    public Date getClaimTime() { return claimTime; }
    public void setClaimTime(Date d) { this.claimTime = d; }
    public Date getEndTime() { return endTime; }
    public void setEndTime(Date d) { this.endTime = d; }
    public Date getDueDate() { return dueDate; }
    public void setDueDate(Date d) { this.dueDate = d; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer p) { this.priority = p; }
    public boolean isSuspended() { return suspended; }
    public void setSuspended(boolean b) { this.suspended = b; }
    public List<String> getBlockedByTaskIds() { return blockedByTaskIds; }
    public void setBlockedByTaskIds(List<String> ids)
    {
        this.blockedByTaskIds = (ids == null) ? Collections.emptyList() : ids;
    }
}
