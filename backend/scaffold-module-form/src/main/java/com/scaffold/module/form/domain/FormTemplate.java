package com.scaffold.module.form.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.scaffold.common.core.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 表单模板：一个可填报的表单定义。
 *
 * <p>核心字段：
 * <ul>
 *   <li>{@link #formKey}：模板对外唯一 key（如 "leave_application"），同 key 多版本可共存</li>
 *   <li>{@link #version}：自增版本号；保存为新草稿时 +1</li>
 *   <li>{@link #status}：DRAFT / PUBLISHED / ARCHIVED；只有 PUBLISHED 可被填报</li>
 *   <li>{@link #schemaJson}：form-create 设计器输出的 rule[] JSON 字符串</li>
 * </ul>
 *
 * <p>软删走 {@code del_flag}（沿用脚手架约定 0=正常 / 2=软删）。
 *
 * @author scaffold
 */
@Schema(description = "表单模板（JSON schema 容器；同 formKey 可多版本共存）")
public class FormTemplate extends BaseEntity
{
    @Schema(description = "主键 ID")
    private Long id;

    @Schema(description = "模板 key（业务侧识别用，同 key 多版本共存）", example = "leave_application")
    private String formKey;

    @Schema(description = "模板名称", example = "请假申请单")
    private String name;

    @Schema(description = "分类（业务分组用，可选）", example = "HR")
    private String category;

    @Schema(description = "form-create rule[] JSON 字符串")
    private String schemaJson;

    @Schema(description = "版本号（自增）", example = "1")
    private Integer version;

    @Schema(description = "状态：DRAFT / PUBLISHED / ARCHIVED", allowableValues = {"DRAFT", "PUBLISHED", "ARCHIVED"})
    private String status;

    @Schema(description = "描述 / 备注")
    private String description;

    @Schema(description = "首次发布时间", format = "date-time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date publishedAt;

    @Schema(description = "软删标记：0=正常 / 2=软删", example = "0")
    private String delFlag;

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
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Date getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Date publishedAt) { this.publishedAt = publishedAt; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
}
