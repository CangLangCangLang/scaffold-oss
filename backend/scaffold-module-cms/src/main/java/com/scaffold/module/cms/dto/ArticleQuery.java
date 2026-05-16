package com.scaffold.module.cms.dto;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 文章列表查询参数（后台 / 公开 API 共用）。
 * <p>
 * 后台：所有字段可空；为空时不参与拼接。<br>
 * 公开 API：{@code status} 由 controller 强制为 {@code PUBLISHED}，{@code del_flag} 由 mapper 强制 {@code '0'}。
 */
@Schema(description = "文章列表查询条件；所有字段可空，为空字段不参与 SQL 拼接")
public class ArticleQuery
{
    @Schema(description = "栏目 id；为空查全部栏目")
    private Long channelId;

    @Schema(description = "文章状态：DRAFT / PENDING / PUBLISHED / UNPUBLISHED；公开 API 中本字段会被强制为 PUBLISHED",
            allowableValues = {"DRAFT", "PENDING", "PUBLISHED", "UNPUBLISHED"})
    private String status;

    @Schema(description = "关键字模糊匹配：title / summary / contentHtml LIKE %keyword%")
    private String keyword;

    @Schema(description = "标签 id；命中即匹配，多标签 OR 暂不支持")
    private Long tagId;

    @Schema(description = "创建者用户名（按精确值过滤）")
    private String createBy;

    @Schema(description = "起始时间（按文章 createTime 过滤），格式 yyyy-MM-dd HH:mm:ss",
            type = "string", example = "2026-01-01 00:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    @Schema(description = "结束时间（按文章 createTime 过滤），格式 yyyy-MM-dd HH:mm:ss",
            type = "string", example = "2026-12-31 23:59:59")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    public Long getChannelId() { return channelId; }

    public void setChannelId(Long channelId) { this.channelId = channelId; }

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }

    public String getKeyword() { return keyword; }

    public void setKeyword(String keyword) { this.keyword = keyword; }

    public Long getTagId() { return tagId; }

    public void setTagId(Long tagId) { this.tagId = tagId; }

    public String getCreateBy() { return createBy; }

    public void setCreateBy(String createBy) { this.createBy = createBy; }

    public Date getStartTime() { return startTime; }

    public void setStartTime(Date startTime) { this.startTime = startTime; }

    public Date getEndTime() { return endTime; }

    public void setEndTime(Date endTime) { this.endTime = endTime; }
}
