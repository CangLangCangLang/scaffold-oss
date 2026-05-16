package com.scaffold.module.report.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 运行日志（每次执行落一条；超时 / 失败也落）。
 *
 * @author scaffold
 */
@Schema(description = "报表 - 运行日志")
public class SysReportRunLog
{
    @Schema(description = "主键 ID")
    private Long id;

    @Schema(description = "模板 ID（即席查询为 null）")
    private Long templateId;

    @Schema(description = "模板编码（冗余）")
    private String templateCode;

    @Schema(description = "数据源 ID（0 = 主库）")
    private Long datasourceId;

    @Schema(description = "SQL 预览（已替换参数；超长截断）")
    private String sqlPreview;

    @Schema(description = "参数 JSON")
    private String paramJson;

    @Schema(description = "返回行数（截断后）")
    private Integer rowCount;

    @Schema(description = "耗时 ms")
    private Long costMs;

    @Schema(description = "状态：0=成功 / 1=失败 / 2=超时", defaultValue = "0")
    private String status;

    @Schema(description = "失败错误（status<>0 时填）")
    private String errorMsg;

    @Schema(description = "执行者 username")
    private String createBy;

    @Schema(description = "执行时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
    public Long getDatasourceId() { return datasourceId; }
    public void setDatasourceId(Long datasourceId) { this.datasourceId = datasourceId; }
    public String getSqlPreview() { return sqlPreview; }
    public void setSqlPreview(String sqlPreview) { this.sqlPreview = sqlPreview; }
    public String getParamJson() { return paramJson; }
    public void setParamJson(String paramJson) { this.paramJson = paramJson; }
    public Integer getRowCount() { return rowCount; }
    public void setRowCount(Integer rowCount) { this.rowCount = rowCount; }
    public Long getCostMs() { return costMs; }
    public void setCostMs(Long costMs) { this.costMs = costMs; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
