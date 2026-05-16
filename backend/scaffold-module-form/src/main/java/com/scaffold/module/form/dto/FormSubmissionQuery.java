package com.scaffold.module.form.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 表单提交记录列表查询。
 *
 * <p>权限隔离规则（service 层强制）：非 admin 仅能看到自己提交的；admin 看全量。
 */
@Schema(description = "表单提交记录列表查询参数")
public class FormSubmissionQuery
{
    @Schema(description = "按模板 ID 过滤")
    private Long templateId;

    @Schema(description = "按模板 key 过滤（与 templateId 互斥可选）")
    private String templateKey;

    @Schema(description = "按提交人过滤；非 admin 调用时被强制覆盖为当前用户")
    private String submitter;

    @Schema(description = "起始时间 yyyy-MM-dd HH:mm:ss")
    private String beginTime;

    @Schema(description = "截止时间 yyyy-MM-dd HH:mm:ss")
    private String endTime;

    @Schema(description = "页码，1-based", example = "1")
    private Integer pageNum;

    @Schema(description = "每页大小，默认 20，最大 200", example = "20")
    private Integer pageSize;

    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public String getTemplateKey() { return templateKey; }
    public void setTemplateKey(String templateKey) { this.templateKey = templateKey; }
    public String getSubmitter() { return submitter; }
    public void setSubmitter(String submitter) { this.submitter = submitter; }
    public String getBeginTime() { return beginTime; }
    public void setBeginTime(String beginTime) { this.beginTime = beginTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public Integer getPageNum() { return pageNum; }
    public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
}
