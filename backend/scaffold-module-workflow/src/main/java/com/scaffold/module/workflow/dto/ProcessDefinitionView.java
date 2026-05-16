package com.scaffold.module.workflow.dto;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 流程定义视图（前端列表用）。
 *
 * @author scaffold
 */
public class ProcessDefinitionView
{
    private String id;
    private String key;
    private String name;
    private Integer version;
    private String description;
    private String resourceName;
    private String deploymentId;
    private boolean suspended;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date deploymentTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getResourceName() { return resourceName; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }
    public String getDeploymentId() { return deploymentId; }
    public void setDeploymentId(String deploymentId) { this.deploymentId = deploymentId; }
    public boolean isSuspended() { return suspended; }
    public void setSuspended(boolean suspended) { this.suspended = suspended; }
    public Date getDeploymentTime() { return deploymentTime; }
    public void setDeploymentTime(Date deploymentTime) { this.deploymentTime = deploymentTime; }
}
