package com.scaffold.module.cms.dto;

/**
 * 列表回填 article-tag 关联用的内部 DTO（mybatis 不直接映射 {@code long[]}）。
 */
public class ArticleTagPair
{
    private Long articleId;
    private Long tagId;

    public Long getArticleId() { return articleId; }

    public void setArticleId(Long articleId) { this.articleId = articleId; }

    public Long getTagId() { return tagId; }

    public void setTagId(Long tagId) { this.tagId = tagId; }
}
