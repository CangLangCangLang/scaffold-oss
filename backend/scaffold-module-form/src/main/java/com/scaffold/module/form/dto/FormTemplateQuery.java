package com.scaffold.module.form.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 表单模板列表查询。
 *
 * <p>不在此处保留默认值，service 层会做 null 安全。
 */
@Schema(description = "表单模板列表查询参数")
public class FormTemplateQuery
{
    @Schema(description = "模板名 LIKE")
    private String keyword;

    @Schema(description = "分类精确匹配")
    private String category;

    @Schema(description = "状态精确匹配（DRAFT / PUBLISHED / ARCHIVED）")
    private String status;

    @Schema(description = "页码，1-based", example = "1")
    private Integer pageNum;

    @Schema(description = "每页大小，默认 20，最大 200", example = "20")
    private Integer pageSize;

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getPageNum() { return pageNum; }
    public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
}
