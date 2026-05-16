package com.scaffold.module.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "报表模板查询条件")
public class TemplateQuery
{
    @Schema(description = "模板名 LIKE")
    private String name;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "创建者 username")
    private String createBy;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
}
