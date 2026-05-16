package com.scaffold.module.cms.domain;

import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.scaffold.common.core.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * CMS 文章。<br>
 * 状态机：{@code DRAFT → PENDING → PUBLISHED → UNPUBLISHED}，再编辑回 {@code DRAFT}。
 * <p>
 * {@code published_at} 仅在首次进入 PUBLISHED 时写入；下线 → 上线不重置，
 * 保留"首次发布时间"语义。
 * <p>
 * {@code tagIds} 不映射到表字段，由 service 层另查 {@code cms_article_tag} 拼上。
 */
@Schema(description = "CMS 文章；既作为后台 CRUD 响应，也作为状态机端点的响应；继承 BaseEntity 含 createBy/createTime/updateBy/updateTime")
public class Article extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_UNPUBLISHED = "UNPUBLISHED";

    @Schema(description = "文章 id")
    private Long id;

    @Schema(description = "所属栏目 id")
    private Long channelId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "公开 URL 段标识，全局唯一")
    private String slug;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "封面图 URL")
    private String coverUrl;

    @Schema(description = "正文 HTML（wangEditor 输出）")
    private String contentHtml;

    @Schema(description = "稿件来源")
    private String source;

    @Schema(description = "作者署名")
    private String author;

    @Schema(description = "状态：DRAFT 草稿 / PENDING 待审核 / PUBLISHED 已发布 / UNPUBLISHED 已下线",
            allowableValues = {"DRAFT", "PENDING", "PUBLISHED", "UNPUBLISHED"})
    private String status;

    @Schema(description = "SEO meta title")
    private String metaTitle;

    @Schema(description = "SEO meta description")
    private String metaDescription;

    @Schema(description = "SEO meta keywords")
    private String metaKeywords;

    @Schema(description = "rel=canonical 链接")
    private String canonicalUrl;

    @Schema(description = "首次发布时间（首次进入 PUBLISHED 时写；下线再上线不重置）", type = "string", example = "2026-05-07 13:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date publishedAt;

    @Schema(description = "公开页阅读量；GET /cms/public/articles/{slug} 命中时事务内 +=1")
    private Long viewCount;

    @Schema(description = "排序权重；数字越小越靠前")
    private Integer sortOrder;

    @Schema(description = "软删标记：'0'=正常，'2'=软删；列表 / 详情 / 公开 API 都强制 '0' 过滤",
            allowableValues = {"0", "2"})
    private String delFlag;

    @Schema(description = "关联的 workflow 流程实例 id；仅当 scaffold-module-cms-workflow 桥模块启用且文章走过 workflow 提交时才有值；CMS 本体不依赖 workflow 类型，仅以 String 存")
    private String processInstanceId;

    @Schema(description = "关联标签 id 列表（service 层从 cms_article_tag 拼上，mapper 不持久化此字段）")
    private List<Long> tagIds;

    @Schema(description = "关联标签详情列表（service 层从 cms_tag 拼上）")
    private List<Tag> tags;

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public Long getChannelId() { return channelId; }

    public void setChannelId(Long channelId) { this.channelId = channelId; }

    public String getTitle() { return title; }

    public void setTitle(String title) { this.title = title; }

    public String getSlug() { return slug; }

    public void setSlug(String slug) { this.slug = slug; }

    public String getSummary() { return summary; }

    public void setSummary(String summary) { this.summary = summary; }

    public String getCoverUrl() { return coverUrl; }

    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public String getContentHtml() { return contentHtml; }

    public void setContentHtml(String contentHtml) { this.contentHtml = contentHtml; }

    public String getSource() { return source; }

    public void setSource(String source) { this.source = source; }

    public String getAuthor() { return author; }

    public void setAuthor(String author) { this.author = author; }

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }

    public String getMetaTitle() { return metaTitle; }

    public void setMetaTitle(String metaTitle) { this.metaTitle = metaTitle; }

    public String getMetaDescription() { return metaDescription; }

    public void setMetaDescription(String metaDescription) { this.metaDescription = metaDescription; }

    public String getMetaKeywords() { return metaKeywords; }

    public void setMetaKeywords(String metaKeywords) { this.metaKeywords = metaKeywords; }

    public String getCanonicalUrl() { return canonicalUrl; }

    public void setCanonicalUrl(String canonicalUrl) { this.canonicalUrl = canonicalUrl; }

    public Date getPublishedAt() { return publishedAt; }

    public void setPublishedAt(Date publishedAt) { this.publishedAt = publishedAt; }

    public Long getViewCount() { return viewCount; }

    public void setViewCount(Long viewCount) { this.viewCount = viewCount; }

    public Integer getSortOrder() { return sortOrder; }

    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public String getDelFlag() { return delFlag; }

    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }

    public List<Long> getTagIds() { return tagIds; }

    public void setTagIds(List<Long> tagIds) { this.tagIds = tagIds; }

    public List<Tag> getTags() { return tags; }

    public void setTags(List<Tag> tags) { this.tags = tags; }

    public String getProcessInstanceId() { return processInstanceId; }

    public void setProcessInstanceId(String processInstanceId) { this.processInstanceId = processInstanceId; }
}
