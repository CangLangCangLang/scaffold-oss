package com.scaffold.module.workflow.dto;

import java.util.Map;

/**
 * 启动流程入参。
 *
 * @author scaffold
 */
public class StartProcessRequest
{
    private String processDefinitionKey;
    private String businessKey;
    private String name;
    private Map<String, Object> variables;

    public String getProcessDefinitionKey() { return processDefinitionKey; }
    public void setProcessDefinitionKey(String s) { this.processDefinitionKey = s; }
    public String getBusinessKey() { return businessKey; }
    public void setBusinessKey(String s) { this.businessKey = s; }
    public String getName() { return name; }
    public void setName(String s) { this.name = s; }
    public Map<String, Object> getVariables() { return variables; }
    public void setVariables(Map<String, Object> v) { this.variables = v; }
}
