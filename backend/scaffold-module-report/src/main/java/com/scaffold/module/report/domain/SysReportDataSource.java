package com.scaffold.module.report.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.scaffold.common.core.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 外部数据源（密码加密存储）。
 *
 * <p>API 返回时密码统一遮蔽为 {@link #passwordMask}，落库时由 service 层用 {@code Aes256Util} 加密
 * 写入 {@link #passwordEnc}（前端永远拿不到明文）。</p>
 *
 * @author scaffold
 */
@Schema(description = "报表 - 外部数据源")
public class SysReportDataSource extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键 ID（0 表示主库虚拟数据源，不入表）")
    private Long id;

    @Schema(description = "数据源编码", example = "warehouse")
    private String code;

    @Schema(description = "展示名")
    private String name;

    @Schema(description = "数据库类型", example = "mysql", allowableValues = {"mysql","postgres","sqlserver","oracle"})
    private String type;

    @Schema(description = "JDBC URL")
    private String jdbcUrl;

    @Schema(description = "驱动全限定名（留空使用类型默认）")
    private String driverClass;

    @Schema(description = "用户名")
    private String username;

    /** AES 密文（不应出现在 API 返回里） */
    @JsonIgnore
    private String passwordEnc;

    @Schema(description = "密码（API 返回时为遮蔽串）")
    private transient String passwordMask;

    @Schema(description = "状态：0=启用 / 1=停用", defaultValue = "0")
    private String status;

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
    public String getPasswordEnc() { return passwordEnc; }
    public void setPasswordEnc(String passwordEnc) { this.passwordEnc = passwordEnc; }
    public String getPasswordMask() { return passwordMask; }
    public void setPasswordMask(String passwordMask) { this.passwordMask = passwordMask; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
