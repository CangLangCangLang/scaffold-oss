package com.scaffold.module.file.domain;

import com.scaffold.common.core.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 文件中心：文件夹（每个用户独立）。
 *
 * <p>结构上是简单两层（根 / 子）— 由 {@link #parentId}=0 表示根；
 * {@link #path} 字段拼接全路径（"/research/papers"），配合 {@code uk_owner_path} 唯一约束保证不重名。
 *
 * @author scaffold
 */
@Schema(description = "文件中心 - 文件夹（用户级隔离）")
public class SysFileFolder extends BaseEntity
{
    @Schema(description = "主键 ID")
    private Long id;

    @Schema(description = "所有者 username")
    private String owner;

    @Schema(description = "父级 ID（0 = 根）", example = "0")
    private Long parentId;

    @Schema(description = "文件夹名", example = "Q3 报表")
    private String name;

    @Schema(description = "从根到本级的全路径", example = "/Q3 报表")
    private String path;

    @Schema(description = "软删标记：0=正常 / 2=软删")
    private String delFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
}
