package com.scaffold.module.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 文件编辑（改名 / 移动文件夹 / 改分类标签）请求体。
 * 不允许改 storagePath / size / mime 等元属性 — 只允许业务展示性字段。
 *
 * @author scaffold
 */
@Schema(description = "文件编辑请求（改名 / 移动 / 标签）")
public class FileEditRequest
{
    @Schema(description = "文件 ID", required = true)
    @NotNull
    private Long id;

    @Schema(description = "新文件名（不改时不传）")
    @Size(max = 255)
    private String name;

    @Schema(description = "新文件夹 ID（0=根；不改时不传）")
    private Long folderId;

    @Schema(description = "分类")
    @Size(max = 64)
    private String category;

    @Schema(description = "标签（逗号分隔）")
    @Size(max = 500)
    private String tags;

    @Schema(description = "备注")
    @Size(max = 500)
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getFolderId() { return folderId; }
    public void setFolderId(Long folderId) { this.folderId = folderId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
