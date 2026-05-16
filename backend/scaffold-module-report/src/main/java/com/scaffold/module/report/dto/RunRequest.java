package com.scaffold.module.report.dto;

import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "运行报表请求")
public class RunRequest
{
    @Schema(description = "模板 ID（与 sql 二选一；优先 templateId）")
    private Long templateId;

    @Schema(description = "即席 SQL（仅运维 / 管理员可用；走同样的 SqlGuard）")
    private String sql;

    @Schema(description = "数据源 ID（即席查询时使用；模板查询忽略此字段）")
    private Long datasourceId;

    @Schema(description = "参数键值对")
    private Map<String, Object> params;

    @Schema(description = "本次运行行数上限（不能超过模板上限或全局 10000）")
    private Integer rowLimit;

    @Schema(description = "本次运行超时 ms（不能超过模板上限或全局 30000）")
    private Integer timeoutMs;

    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public String getSql() { return sql; }
    public void setSql(String sql) { this.sql = sql; }
    public Long getDatasourceId() { return datasourceId; }
    public void setDatasourceId(Long datasourceId) { this.datasourceId = datasourceId; }
    public Map<String, Object> getParams() { return params; }
    public void setParams(Map<String, Object> params) { this.params = params; }
    public Integer getRowLimit() { return rowLimit; }
    public void setRowLimit(Integer rowLimit) { this.rowLimit = rowLimit; }
    public Integer getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(Integer timeoutMs) { this.timeoutMs = timeoutMs; }
}
