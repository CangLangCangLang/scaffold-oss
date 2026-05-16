package com.scaffold.module.file.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.scaffold.common.core.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 文件中心：单个上传文件记录。
 *
 * <p>一行 = 一次成功的物理文件落盘。物理路径在 {@link #storagePath}（由 framework 的
 * {@code UploadStorageService} 返回，本模块不感知存储介质 — 本地 / S3 透明）。
 *
 * <h3>软删与硬删</h3>
 * 软删 → {@link #delFlag}=2 + {@link #deleteTime}=now()，可后台恢复；
 * 30 天后由 quartz 定时任务调 {@code FileService.purgeExpired()}：调 framework 删物理 + 删 DB。
 *
 * <h3>引用计数</h3>
 * {@link #refCount} >0 时禁止删除（CMS 文章 / Form 表单 把本文件挂上时累加），保护跨模块挂引用。
 *
 * @author scaffold
 */
@Schema(description = "文件中心 - 上传文件主记录")
public class SysFile extends BaseEntity
{
    @Schema(description = "主键 ID")
    private Long id;

    @Schema(description = "业务桶（cms/image / form / common 等）", example = "common")
    private String bucket;

    @Schema(description = "所属文件夹 ID（NULL = 根）")
    private Long folderId;

    @Schema(description = "文件展示名（用户可改）", example = "Q3 销售报表.pdf")
    private String name;

    @Schema(description = "上传时原始文件名")
    private String originalName;

    @Schema(description = "扩展名（小写、不含点）", example = "pdf")
    private String ext;

    @Schema(description = "MIME 类型", example = "application/pdf")
    private String mime;

    @Schema(description = "文件字节数", example = "10240")
    private Long sizeBytes;

    @Schema(description = "物理存储路径（FileStorageService 返回的相对 URL）")
    private String storagePath;

    @Schema(description = "分类标签（用户自定义）")
    private String category;

    @Schema(description = "逗号分隔的标签列表", example = "财务,2026Q3")
    private String tags;

    @Schema(description = "跨模块引用计数（>0 时禁删）", example = "0")
    private Integer refCount;

    @Schema(description = "软删标记：0=正常 / 2=软删", example = "0")
    private String delFlag;

    @Schema(description = "软删时间（30 天后由 quartz 物理清磁盘）", format = "date-time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date deleteTime;

    @Schema(description = "上传者昵称（冗余，避免列表 join sys_user）")
    private String createByName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }
    public Long getFolderId() { return folderId; }
    public void setFolderId(Long folderId) { this.folderId = folderId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public String getExt() { return ext; }
    public void setExt(String ext) { this.ext = ext; }
    public String getMime() { return mime; }
    public void setMime(String mime) { this.mime = mime; }
    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public Integer getRefCount() { return refCount; }
    public void setRefCount(Integer refCount) { this.refCount = refCount; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
    public Date getDeleteTime() { return deleteTime; }
    public void setDeleteTime(Date deleteTime) { this.deleteTime = deleteTime; }
    public String getCreateByName() { return createByName; }
    public void setCreateByName(String createByName) { this.createByName = createByName; }
}
