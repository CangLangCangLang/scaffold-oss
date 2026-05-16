package com.scaffold.module.workflow.dto;

import java.util.Map;

/**
 * 完成任务入参。
 * <p>
 * variables 与 formData 都会作为流程变量提交：
 * <ul>
 *   <li>{@code variables}：业务侧手工指定的"系统变量"，例如审批结果布尔值、流程跳转开关；</li>
 *   <li>{@code formData}：动态表单（form-create / 自定义）渲染并校验后产出的字段集合，
 *       内容由 {@code wf_form_schema} 决定。</li>
 * </ul>
 * 提交时会合并到一个 Map 写回引擎；同名 key 时 {@code variables} 覆盖 {@code formData}（系统变量优先）。
 *
 * @author scaffold
 */
public class CompleteTaskRequest
{
    /** 完成意见（写入历史评论） */
    private String comment;
    /** 由业务侧手工指定的流程变量；可空 */
    private Map<String, Object> variables;
    /** 由动态表单产出的字段集合；可空 */
    private Map<String, Object> formData;

    public String getComment() { return comment; }
    public void setComment(String s) { this.comment = s; }
    public Map<String, Object> getVariables() { return variables; }
    public void setVariables(Map<String, Object> v) { this.variables = v; }
    public Map<String, Object> getFormData() { return formData; }
    public void setFormData(Map<String, Object> formData) { this.formData = formData; }
}
