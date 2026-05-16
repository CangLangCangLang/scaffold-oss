package com.scaffold.module.cms.dto;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 文章新建 / 编辑请求体。<br>
 * 状态字段不在这里——状态由 6 个流转端点单独控制（第 2 批）。
 */
@Schema(description = "文章新建 / 编辑请求体；状态机字段不在此结构中（用 /cms/article/{id}/{action} 单独流转）")
public class ArticleSaveRequest
{
    @Schema(description = "文章 id；新建留空，编辑时必填")
    private Long id;

    @Schema(description = "所属栏目 id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long channelId;

    @Schema(description = "文章标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "脚手架 M-3 上线公告")
    private String title;

    @Schema(description = "公开 URL 段标识；留空时由后端按 title 自动生成（中文降级为 article-{8 位 UUID}）；全局唯一",
            example = "scaffold-m3-release")
    private String slug;

    @Schema(description = "摘要（列表页展示）；可空")
    private String summary;

    @Schema(description = "封面图 URL；建议走 /cms/upload/image 拿到的 URL", example = "/profile/cms/image/202605/abcd.jpg")
    private String coverUrl;

    @Schema(description = "文章正文（HTML 格式，由 wangEditor 输出）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String contentHtml;

    @Schema(description = "稿件来源（如「转载自 XX 站」），可空")
    private String source;

    @Schema(description = "作者署名（展示用）；与 createBy（创建者账号）解耦——可一致也可不一致")
    private String author;

    @Schema(description = "SEO meta title；不传则用 title")
    private String metaTitle;

    @Schema(description = "SEO meta description；不传则用 summary")
    private String metaDescription;

    @Schema(description = "SEO meta keywords；逗号分隔")
    private String metaKeywords;

    @Schema(description = "规范链接（rel=canonical），用于跨域去重；可空")
    private String canonicalUrl;

    @Schema(description = "排序权重；数字越小越靠前（默认 0）", example = "0")
    private Integer sortOrder;

    @Schema(description = "标签 id 列表（已存在的 cms_tag.id），保存时会重写 cms_article_tag 关联")
    private List<Long> tagIds;

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

    public String getMetaTitle() { return metaTitle; }

    public void setMetaTitle(String metaTitle) { this.metaTitle = metaTitle; }

    public String getMetaDescription() { return metaDescription; }

    public void setMetaDescription(String metaDescription) { this.metaDescription = metaDescription; }

    public String getMetaKeywords() { return metaKeywords; }

    public void setMetaKeywords(String metaKeywords) { this.metaKeywords = metaKeywords; }

    public String getCanonicalUrl() { return canonicalUrl; }

    public void setCanonicalUrl(String canonicalUrl) { this.canonicalUrl = canonicalUrl; }

    public Integer getSortOrder() { return sortOrder; }

    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public List<Long> getTagIds() { return tagIds; }

    public void setTagIds(List<Long> tagIds) { this.tagIds = tagIds; }
}
