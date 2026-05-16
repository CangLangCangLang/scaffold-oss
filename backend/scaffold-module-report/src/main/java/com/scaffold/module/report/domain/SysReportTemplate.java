package com.scaffold.module.report.domain;

import com.scaffold.common.core.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 报表 SQL 模板。
 *
 * @author scaffold
 */
@Schema(description = "报表 - SQL 模板")
public class SysReportTemplate extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键 ID")
    private Long id;

    @Schema(description = "业务编码（唯一）", example = "user_growth_daily")
    private String code;

    @Schema(description = "模板名")
    private String name;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "数据源 ID（0 = 主库）", defaultValue = "0")
    private Long datasourceId;

    @Schema(description = "SELECT 模板（参数 ${name} 占位）")
    private String sqlText;

    @Schema(description = "参数声明 JSON：[{name,type,label,required,default}]")
    private String paramSchema;

    @Schema(description = "行数上限（≤ 10000）", defaultValue = "10000")
    private Integer rowLimit;

    @Schema(description = "查询超时 ms（≤ 30000）", defaultValue = "30000")
    private Integer timeoutMs;

    @Schema(description = "运行此模板需要的权限 key（可空 = 仅登录）")
    private String permKey;

    @Schema(description = "状态：0=启用 / 1=停用", defaultValue = "0")
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Long getDatasourceId() { return datasourceId; }
    public void setDatasourceId(Long datasourceId) { this.datasourceId = datasourceId; }
    public String getSqlText() { return sqlText; }
    public void setSqlText(String sqlText) { this.sqlText = sqlText; }
    public String getParamSchema() { return paramSchema; }
    public void setParamSchema(String paramSchema) { this.paramSchema = paramSchema; }
    public Integer getRowLimit() { return rowLimit; }
    public void setRowLimit(Integer rowLimit) { this.rowLimit = rowLimit; }
    public Integer getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(Integer timeoutMs) { this.timeoutMs = timeoutMs; }
    public String getPermKey() { return permKey; }
    public void setPermKey(String permKey) { this.permKey = permKey; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
