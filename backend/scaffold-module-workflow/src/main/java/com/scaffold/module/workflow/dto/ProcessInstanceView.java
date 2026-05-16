package com.scaffold.module.workflow.dto;

import java.util.Date;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 流程实例视图。
 *
 * @author scaffold
 */
public class ProcessInstanceView
{
    private String id;
    private String processDefinitionId;
    private String processDefinitionKey;
    private String processDefinitionName;
    private String businessKey;
    private String startUserId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
    private String activityId;
    private boolean ended;
    private boolean suspended;
    private Map<String, Object> variables;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProcessDefinitionId() { return processDefinitionId; }
    public void setProcessDefinitionId(String s) { this.processDefinitionId = s; }
    public String getProcessDefinitionKey() { return processDefinitionKey; }
    public void setProcessDefinitionKey(String s) { this.processDefinitionKey = s; }
    public String getProcessDefinitionName() { return processDefinitionName; }
    public void setProcessDefinitionName(String s) { this.processDefinitionName = s; }
    public String getBusinessKey() { return businessKey; }
    public void setBusinessKey(String s) { this.businessKey = s; }
    public String getStartUserId() { return startUserId; }
    public void setStartUserId(String s) { this.startUserId = s; }
    public Date getStartTime() { return startTime; }
    public void setStartTime(Date d) { this.startTime = d; }
    public Date getEndTime() { return endTime; }
    public void setEndTime(Date d) { this.endTime = d; }
    public String getActivityId() { return activityId; }
    public void setActivityId(String s) { this.activityId = s; }
    public boolean isEnded() { return ended; }
    public void setEnded(boolean b) { this.ended = b; }
    public boolean isSuspended() { return suspended; }
    public void setSuspended(boolean b) { this.suspended = b; }
    public Map<String, Object> getVariables() { return variables; }
    public void setVariables(Map<String, Object> v) { this.variables = v; }
}
