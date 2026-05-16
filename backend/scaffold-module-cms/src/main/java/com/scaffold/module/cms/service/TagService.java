package com.scaffold.module.cms.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.scaffold.common.exception.ServiceException;
import com.scaffold.common.utils.SecurityUtils;
import com.scaffold.common.utils.StringUtils;
import com.scaffold.module.cms.domain.Tag;
import com.scaffold.module.cms.mapper.TagMapper;

@Service
public class TagService
{
    @Autowired private TagMapper tagMapper;

    public List<Tag> list(String name)
    {
        return tagMapper.selectList(name);
    }

    public Tag getById(Long id)
    {
        Tag t = tagMapper.selectById(id);
        if (t == null) throw new ServiceException("标签不存在: " + id);
        return t;
    }

    @Transactional
    public Tag create(Tag form)
    {
        if (StringUtils.isEmpty(form.getName())) throw new ServiceException("标签名称不能为空");
        if (tagMapper.selectByName(form.getName()) != null)
        {
            throw new ServiceException("标签名称已存在: " + form.getName());
        }
        if (form.getColor() == null) form.setColor("");
        form.setCreateBy(SecurityUtils.getUsername());
        tagMapper.insert(form);
        return form;
    }

    @Transactional
    public Tag update(Tag form)
    {
        if (form.getId() == null) throw new ServiceException("缺少 id");
        Tag exist = getById(form.getId());
        if (StringUtils.isNotEmpty(form.getName()) && !form.getName().equals(exist.getName()))
        {
            Tag byName = tagMapper.selectByName(form.getName());
            if (byName != null && !byName.getId().equals(exist.getId()))
            {
                throw new ServiceException("标签名称已存在: " + form.getName());
            }
        }
        tagMapper.updateById(form);
        return getById(form.getId());
    }

    @Transactional
    public void delete(Long id)
    {
        getById(id);
        // 先清 article-tag 关联，再删 tag 本体；避免列表回填时出现 tagId 命中 cms_article_tag
        // 但 tag 已不存在的孤儿行。
        tagMapper.deleteArticleTagByTagId(id);
        tagMapper.deleteById(id);
    }
}
