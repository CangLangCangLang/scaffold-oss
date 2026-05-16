package com.scaffold.module.file.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 文件中心：跨模块引用记录（一行 = 一个业务记录引用一个文件）。
 *
 * <p>把 {@code (file_id, ref_module, ref_type, ref_id)} 设为唯一键 — 同一对象 (例如 article=42) 引用
 * 同一个文件不会双计数。{@code FileRefService.attach()} 会先 SELECT 再 INSERT IGNORE 保证幂等。
 *
 * <p>典型用法：CMS 文章发布时：
 * <pre>
 *   fileRefService.attach(fileId, "cms", "article", articleId.toString());
 *   // ... 文章下线时
 *   fileRefService.detach(fileId, "cms", "article", articleId.toString());
 * </pre>
 *
 * @author scaffold
 */
@Schema(description = "文件中心 - 跨模块引用计数明细")
public class SysFileRef
{
    @Schema(description = "主键 ID")
    private Long id;

    @Schema(description = "引用的 sys_file.id", example = "12")
    private Long fileId;

    @Schema(description = "引用方模块（cms / form / wf 等）", example = "cms")
    private String refModule;

    @Schema(description = "引用方业务类型（article / submission 等）", example = "article")
    private String refType;

    @Schema(description = "引用方业务记录 ID（字符串，可适配 UUID 等）", example = "42")
    private String refId;

    @Schema(description = "建立引用的用户")
    private String createBy;

    @Schema(description = "建立引用时间", format = "date-time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFileId() { return fileId; }
    public void setFileId(Long fileId) { this.fileId = fileId; }
    public String getRefModule() { return refModule; }
    public void setRefModule(String refModule) { this.refModule = refModule; }
    public String getRefType() { return refType; }
    public void setRefType(String refType) { this.refType = refType; }
    public String getRefId() { return refId; }
    public void setRefId(String refId) { this.refId = refId; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
