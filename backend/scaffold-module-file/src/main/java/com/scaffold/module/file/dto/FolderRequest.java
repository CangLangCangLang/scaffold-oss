package com.scaffold.module.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建 / 改名文件夹请求体。
 *
 * @author scaffold
 */
@Schema(description = "文件夹创建 / 改名请求")
public class FolderRequest
{
    @Schema(description = "文件夹 ID（新建时不传，改名时必填）")
    private Long id;

    @Schema(description = "父级 ID（新建时必填；0 = 根）", example = "0")
    private Long parentId;

    @Schema(description = "文件夹名", required = true, example = "Q3 报表")
    @NotBlank
    @Size(max = 128)
    private String name;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
