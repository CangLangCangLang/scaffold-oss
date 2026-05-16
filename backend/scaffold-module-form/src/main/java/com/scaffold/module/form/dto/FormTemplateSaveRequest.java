package com.scaffold.module.form.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 表单模板保存请求（add / edit 共用）。
 * <p>更新时把 id 带上，新增时 id=null；formKey 一旦确定不可改（与历史提交记录的 templateKey 解耦防误更）。
 */
@Schema(description = "表单模板保存请求")
public class FormTemplateSaveRequest
{
    @Schema(description = "主键 ID（新增不传，编辑必传）")
    private Long id;

    @Schema(description = "模板 key（新增必填，编辑忽略）", example = "leave_application")
    private String formKey;

    @Schema(description = "模板名称", required = true)
    private String name;

    @Schema(description = "分类（HR / 财务 / IT 等）")
    private String category;

    @Schema(description = "form-create rule[] JSON 字符串", required = true)
    private String schemaJson;

    @Schema(description = "描述 / 备注")
    private String description;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFormKey() { return formKey; }
    public void setFormKey(String formKey) { this.formKey = formKey; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSchemaJson() { return schemaJson; }
    public void setSchemaJson(String schemaJson) { this.schemaJson = schemaJson; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
