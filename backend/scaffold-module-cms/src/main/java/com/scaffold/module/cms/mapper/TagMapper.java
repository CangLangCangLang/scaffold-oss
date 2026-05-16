package com.scaffold.module.cms.mapper;

import java.util.List;
import java.util.Set;
import org.apache.ibatis.annotations.Param;
import com.scaffold.module.cms.domain.Tag;

public interface TagMapper
{
    List<Tag> selectList(@Param("name") String nameLike);

    Tag selectById(@Param("id") Long id);

    Tag selectByName(@Param("name") String name);

    int insert(Tag tag);

    int updateById(Tag tag);

    int deleteById(@Param("id") Long id);

    /** 取多个 id 的标签详情，用于 article 列表回填。 */
    List<Tag> selectByIds(@Param("ids") Set<Long> ids);

    /** 同步清掉 cms_article_tag 中引用该 tag 的所有行（避免孤儿关联）。 */
    int deleteArticleTagByTagId(@Param("tagId") Long tagId);

    /** 该 tag 关联的文章数量（删 tag 时仅做提示用，不强制阻拦）。 */
    int countArticleByTagId(@Param("tagId") Long tagId);
}
