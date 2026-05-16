package com.scaffold.module.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 文件列表查询条件（DTO；mapper 用 Map 也行，但 DTO 给 OpenAPI 自动文档更好看）。
 *
 * <p>有 {@code file:list} 权限时不强制 {@link #createBy}；
 * 仅 {@code file:list:mine} 时由 controller 自动塞当前用户。
 *
 * @author scaffold
 */
@Schema(description = "文件列表查询条件")
public class FileQuery
{
    @Schema(description = "文件名 LIKE")
    private String name;

    @Schema(description = "上传者 username（不传 = 不限）")
    private String createBy;

    @Schema(description = "桶（cms/image / form / common 等）")
    private String bucket;

    @Schema(description = "扩展名（小写）", example = "pdf")
    private String ext;

    @Schema(description = "MIME 前缀匹配", example = "image/")
    private String mime;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "所属文件夹 ID（NULL = 不限；0 = 根；其它 = 具体文件夹）")
    private Long folderId;

    @Schema(description = "起始上传时间（yyyy-MM-dd HH:mm:ss 或前端原样）")
    private String beginTime;

    @Schema(description = "结束上传时间")
    private String endTime;

    @Schema(description = "字节数下限（>=）")
    private Long minBytes;

    @Schema(description = "字节数上限（<=）")
    private Long maxBytes;

    @Schema(description = "del_flag：默认只看 0=正常；传 2 看回收站", allowableValues = {"0", "2"})
    private String delFlag;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }
    public String getExt() { return ext; }
    public void setExt(String ext) { this.ext = ext; }
    public String getMime() { return mime; }
    public void setMime(String mime) { this.mime = mime; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Long getFolderId() { return folderId; }
    public void setFolderId(Long folderId) { this.folderId = folderId; }
    public String getBeginTime() { return beginTime; }
    public void setBeginTime(String beginTime) { this.beginTime = beginTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public Long getMinBytes() { return minBytes; }
    public void setMinBytes(Long minBytes) { this.minBytes = minBytes; }
    public Long getMaxBytes() { return maxBytes; }
    public void setMaxBytes(Long maxBytes) { this.maxBytes = maxBytes; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
}
