package com.scaffold.module.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "数据源新增 / 编辑请求")
public class DataSourceUpsertRequest
{
    @Schema(description = "主键 ID（编辑时必填）")
    private Long id;

    @Schema(description = "数据源编码（唯一）", required = true)
    private String code;

    @Schema(description = "展示名", required = true)
    private String name;

    @Schema(description = "类型", example = "mysql")
    private String type;

    @Schema(description = "JDBC URL", required = true)
    private String jdbcUrl;

    @Schema(description = "驱动全限定名（留空使用类型默认）")
    private String driverClass;

    @Schema(description = "用户名")
    private String username;

    /**
     * 密码明文：编辑时仅当字段非 null 才更新；空字符串视为清空；null 视为不动。
     * 后端用 Aes256Util 加密落库。
     */
    @Schema(description = "密码明文（仅写；编辑时 null 表示不修改，空串表示清空）")
    private String password;

    @Schema(description = "状态：0=启用 / 1=停用")
    private String status;

    @Schema(description = "备注")
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getJdbcUrl() { return jdbcUrl; }
    public void setJdbcUrl(String jdbcUrl) { this.jdbcUrl = jdbcUrl; }
    public String getDriverClass() { return driverClass; }
    public void setDriverClass(String driverClass) { this.driverClass = driverClass; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
