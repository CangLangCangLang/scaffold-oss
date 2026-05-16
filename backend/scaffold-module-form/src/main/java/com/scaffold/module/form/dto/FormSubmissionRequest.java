package com.scaffold.module.form.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 表单填报提交请求。
 *
 * <p>data 是已通过前端 form-create 校验后的 JSON 字符串；后端会再做 schema 必填项校验
 * （仅 P1：检查必填字段非空；复杂正则 / 跨字段联动留给前端）。
 */
@Schema(description = "表单填报提交请求")
public class FormSubmissionRequest
{
    @Schema(description = "目标模板 ID", required = true)
    private Long templateId;

    @Schema(description = "业务字段 JSON 字符串（与 schema 对应）", required = true)
    private String data;

    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
}
