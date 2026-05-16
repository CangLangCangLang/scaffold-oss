package com.scaffold.module.workflow.domain;

import java.util.Date;

/**
 * 工作流动态表单 schema 模板存档。
 * <p>
 * 设计：按 (process_definition_key, activity_id) 唯一索引；同一对 (key, activityId) 可有
 * 多个版本，启用的最新版本为当前生效。activityId 取常量 {@link #ACTIVITY_START_FORM} 表示
 * 流程的启动表单。
 *
 * @author scaffold
 */
public class WfFormSchema
{
    /** 启动表单的 activityId 占位值（不与 BPMN 节点冲突，故用双下划线） */
    public static final String ACTIVITY_START_FORM = "__START__";

    private Long id;
    private String processDefinitionKey;
    private String activityId;
    private String name;
    private Integer version;
    private String schemaJson;
    private Boolean enabled;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProcessDefinitionKey() { return processDefinitionKey; }
    public void setProcessDefinitionKey(String v) { this.processDefinitionKey = v; }

    public String getActivityId() { return activityId; }
    public void setActivityId(String v) { this.activityId = v; }

    public String getName() { return name; }
    public void setName(String v) { this.name = v; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer v) { this.version = v; }

    public String getSchemaJson() { return schemaJson; }
    public void setSchemaJson(String v) { this.schemaJson = v; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean v) { this.enabled = v; }

    public String getCreateBy() { return createBy; }
    public void setCreateBy(String v) { this.createBy = v; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date v) { this.createTime = v; }

    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String v) { this.updateBy = v; }

    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date v) { this.updateTime = v; }
}
