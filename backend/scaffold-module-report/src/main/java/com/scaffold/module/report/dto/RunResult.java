package com.scaffold.module.report.dto;

import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "运行结果（前端渲染表格 / 图表共用）")
public class RunResult
{
    @Schema(description = "列名（按 SQL 返回顺序）")
    private List<String> columns;

    @Schema(description = "列 JDBC 类型（前端格式化用）")
    private List<String> columnTypes;

    @Schema(description = "行数据（每行 = 列值数组）")
    private List<List<Object>> rows;

    @Schema(description = "返回行数（已按 rowLimit 截断）")
    private int rowCount;

    @Schema(description = "是否被行数截断")
    private boolean truncated;

    @Schema(description = "执行耗时 ms")
    private long costMs;

    @Schema(description = "已替换参数后的 SQL 预览")
    private String sqlPreview;

    @Schema(description = "实际参数序列（PreparedStatement 顺序参数）")
    private List<Object> boundValues;

    @Schema(description = "运行日志 ID（落库后回填，可用于审计跳转）")
    private Long runLogId;

    public static RunResult of(List<String> cols, List<String> types, List<List<Object>> rows,
                               long costMs, boolean truncated, String sqlPreview,
                               List<Object> bound)
    {
        RunResult r = new RunResult();
        r.columns = cols;
        r.columnTypes = types;
        r.rows = rows;
        r.rowCount = rows.size();
        r.costMs = costMs;
        r.truncated = truncated;
        r.sqlPreview = sqlPreview;
        r.boundValues = bound;
        return r;
    }

    public Map<String, Object> meta()
    {
        return Map.of(
                "columns", columns,
                "columnTypes", columnTypes,
                "rowCount", rowCount,
                "truncated", truncated,
                "costMs", costMs);
    }

    public List<String> getColumns() { return columns; }
    public void setColumns(List<String> columns) { this.columns = columns; }
    public List<String> getColumnTypes() { return columnTypes; }
    public void setColumnTypes(List<String> columnTypes) { this.columnTypes = columnTypes; }
    public List<List<Object>> getRows() { return rows; }
    public void setRows(List<List<Object>> rows) { this.rows = rows; }
    public int getRowCount() { return rowCount; }
    public void setRowCount(int rowCount) { this.rowCount = rowCount; }
    public boolean isTruncated() { return truncated; }
    public void setTruncated(boolean truncated) { this.truncated = truncated; }
    public long getCostMs() { return costMs; }
    public void setCostMs(long costMs) { this.costMs = costMs; }
    public String getSqlPreview() { return sqlPreview; }
    public void setSqlPreview(String sqlPreview) { this.sqlPreview = sqlPreview; }
    public List<Object> getBoundValues() { return boundValues; }
    public void setBoundValues(List<Object> boundValues) { this.boundValues = boundValues; }
    public Long getRunLogId() { return runLogId; }
    public void setRunLogId(Long runLogId) { this.runLogId = runLogId; }
}
