package com.scaffold.module.report.domain;

import com.scaffold.common.core.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 看板。
 *
 * @author scaffold
 */
@Schema(description = "报表 - 看板")
public class SysReportDashboard extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键 ID")
    private Long id;

    @Schema(description = "业务编码（唯一）")
    private String code;

    @Schema(description = "看板名")
    private String name;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "布局 JSON")
    private String layoutJson;

    @Schema(description = "查看权限 key（可空 = 仅登录）")
    private String permKey;

    @Schema(description = "状态：0=启用 / 1=停用")
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getLayoutJson() { return layoutJson; }
    public void setLayoutJson(String layoutJson) { this.layoutJson = layoutJson; }
    public String getPermKey() { return permKey; }
    public void setPermKey(String permKey) { this.permKey = permKey; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
