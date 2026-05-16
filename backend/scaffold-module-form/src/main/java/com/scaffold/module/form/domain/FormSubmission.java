package com.scaffold.module.form.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 表单提交记录（混合存储模型）：
 * <ul>
 *   <li>平台元数据列：submitter / status / createTime / updateTime —— 跑索引、做权限隔离</li>
 *   <li>业务字段：data 列存 JSON 字符串 —— 跟 schemaJson 走</li>
 * </ul>
 *
 * <p>设计取舍：不为每个表单建独立 DB 表（避免动态 DDL 维护成本），
 * 通用查询走 元数据列 + JSON 提取（MySQL 5.7+ 支持 {@code ->>}）；
 * 高级报表场景可在后续基于 form_submission 视图加字段抽取层。
 *
 * @author scaffold
 */
@Schema(description = "表单提交记录（混合存储：平台元数据列 + JSON data 列）")
public class FormSubmission
{
    @Schema(description = "主键 ID")
    private Long id;

    @Schema(description = "所属模板 ID")
    private Long templateId;

    @Schema(description = "模板 key（冗余存储以便不连模板表也能渲染列表）")
    private String templateKey;

    @Schema(description = "模板版本号（提交瞬间快照，模板新版不影响旧记录）")
    private Integer templateVersion;

    @Schema(description = "提交人（用户名 username）")
    private String submitter;

    @Schema(description = "提交人显示名（昵称，记录时冗余以便表格直接展示）")
    private String submitterName;

    @Schema(description = "状态：SUBMITTED（已提交，默认）")
    private String status;

    @Schema(description = "业务字段 JSON（与 form schema 对应）")
    private String data;

    @Schema(description = "提交时间", format = "date-time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @Schema(description = "更新时间", format = "date-time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public String getTemplateKey() { return templateKey; }
    public void setTemplateKey(String templateKey) { this.templateKey = templateKey; }
    public Integer getTemplateVersion() { return templateVersion; }
    public void setTemplateVersion(Integer templateVersion) { this.templateVersion = templateVersion; }
    public String getSubmitter() { return submitter; }
    public void setSubmitter(String submitter) { this.submitter = submitter; }
    public String getSubmitterName() { return submitterName; }
    public void setSubmitterName(String submitterName) { this.submitterName = submitterName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
