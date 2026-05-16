package com.scaffold.module.cms.domain;

import com.scaffold.common.core.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * CMS 标签字典。{@code name} 唯一。
 */
@Schema(description = "CMS 标签字典项；name 全局唯一；继承 BaseEntity 含 createBy/createTime/updateBy/updateTime")
public class Tag extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Schema(description = "标签 id；新建留空，编辑必填")
    private Long id;

    @Schema(description = "标签名称；全局唯一", requiredMode = Schema.RequiredMode.REQUIRED, example = "重要")
    private String name;

    @Schema(description = "标签色值（hex 字符串），列表展示用", example = "#f56c6c")
    private String color;

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getColor() { return color; }

    public void setColor(String color) { this.color = color; }
}
