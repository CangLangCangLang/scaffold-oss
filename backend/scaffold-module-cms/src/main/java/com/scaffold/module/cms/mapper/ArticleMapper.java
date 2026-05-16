package com.scaffold.module.cms.mapper;

import java.util.List;
import java.util.Set;
import org.apache.ibatis.annotations.Param;
import com.scaffold.module.cms.domain.Article;
import com.scaffold.module.cms.dto.ArticleQuery;
import com.scaffold.module.cms.dto.ArticleTagPair;

/**
 * 文章 mapper。所有 select 默认强制 {@code del_flag='0'}。<br>
 * 富文本正文 {@code content_html} 在列表查询里**不会返回**（节省网络），仅 {@code selectById} 拿全量。
 */
public interface ArticleMapper
{
    /** 后台分页（不返回 content_html）。pagehelper 会拦截分页。 */
    List<Article> selectAdminList(ArticleQuery query);

    /** 公开门户分页：仅 PUBLISHED + del_flag='0'，不返回 content_html。 */
    List<Article> selectPublicList(ArticleQuery query);

    /** 后台详情：返回全量，含正文。 */
    Article selectById(@Param("id") Long id);

    /** 公开门户详情：仅 PUBLISHED + del_flag='0'。 */
    Article selectPublicBySlug(@Param("slug") String slug);

    Article selectBySlug(@Param("slug") String slug);

    int insert(Article article);

    int updateById(Article article);

    /** 仅切换状态 + published_at + 操作人 + 时间戳，不动正文。 */
    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("publishedAt") java.util.Date publishedAt,
                     @Param("updateBy") String updateBy);

    /**
     * 仅回写 process_instance_id（M-4 cms-workflow 桥模块在 startProcess 之后调用）。
     * 传入 null 即清空（流程结束后由桥模块清理；保留也无副作用）。
     */
    int updateProcessInstanceId(@Param("id") Long id,
                                @Param("processInstanceId") String processInstanceId);

    int softDelete(@Param("id") Long id, @Param("updateBy") String updateBy);

    /** 阅读量 +1（公开 API 调用）。 */
    int incrementViewCount(@Param("id") Long id);

    int countByChannelId(@Param("channelId") Long channelId);

    /** ===== 文章-标签关联 ===== */

    /** 给指定文章 id 集合一次性拿到 article_id -> tag_id 的全部映射，用于列表回填。 */
    List<ArticleTagPair> selectArticleTagPairs(@Param("articleIds") Set<Long> articleIds);

    /** 单文章的 tagId 列表（详情页 + 编辑页用）。 */
    List<Long> selectTagIdsByArticleId(@Param("articleId") Long articleId);

    int deleteTagsByArticleId(@Param("articleId") Long articleId);

    int batchInsertTags(@Param("articleId") Long articleId, @Param("tagIds") List<Long> tagIds);
}
