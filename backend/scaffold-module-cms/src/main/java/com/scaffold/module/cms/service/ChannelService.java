package com.scaffold.module.cms.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.scaffold.common.exception.ServiceException;
import com.scaffold.common.utils.SecurityUtils;
import com.scaffold.common.utils.StringUtils;
import com.scaffold.module.cms.domain.Channel;
import com.scaffold.module.cms.dto.ChannelTreeNode;
import com.scaffold.module.cms.mapper.ArticleMapper;
import com.scaffold.module.cms.mapper.ChannelMapper;

/**
 * CMS 栏目服务。
 * <ul>
 *   <li>code 全局唯一（含软删 — 看 mapper 有没有过滤；这里 mapper 已限制 del_flag='0'，
 *       所以"软删后再插入同 code"是允许的）。</li>
 *   <li>不允许把栏目 parent 改成自己或自己的子孙——会形成环。</li>
 *   <li>删除前会校验是否还有正常子栏目 / 文章；都为 0 才能删。</li>
 * </ul>
 */
@Service
public class ChannelService
{
    private static final Logger log = LoggerFactory.getLogger(ChannelService.class);

    @Autowired private ChannelMapper channelMapper;
    @Autowired private ArticleMapper articleMapper;

    public List<Channel> list(Channel filter)
    {
        return channelMapper.selectList(filter == null ? new Channel() : filter);
    }

    public List<ChannelTreeNode> tree(boolean activeOnly)
    {
        List<Channel> rows = activeOnly ? channelMapper.selectActiveList()
                                        : channelMapper.selectList(new Channel());
        return buildTree(rows);
    }

    public Channel getById(Long id)
    {
        Channel c = channelMapper.selectById(id);
        if (c == null) throw new ServiceException("栏目不存在或已删除: " + id);
        return c;
    }

    public Channel getByCode(String code)
    {
        return channelMapper.selectByCode(code);
    }

    @Transactional
    public Channel create(Channel form)
    {
        validateCommon(form, null);
        if (channelMapper.selectByCode(form.getCode()) != null)
        {
            throw new ServiceException("栏目编码已存在: " + form.getCode());
        }
        if (form.getParentId() == null) form.setParentId(0L);
        if (form.getOrderNum() == null) form.setOrderNum(0);
        if (form.getStatus() == null) form.setStatus("0");
        form.setCreateBy(SecurityUtils.getUsername());
        channelMapper.insert(form);
        log.info("CMS 栏目已创建 id={} code={} name={}", form.getId(), form.getCode(), form.getName());
        return form;
    }

    @Transactional
    public Channel update(Channel form)
    {
        if (form.getId() == null) throw new ServiceException("缺少 id");
        Channel exist = getById(form.getId());
        validateCommon(form, exist.getId());

        if (StringUtils.isNotEmpty(form.getCode()) && !form.getCode().equals(exist.getCode()))
        {
            Channel byCode = channelMapper.selectByCode(form.getCode());
            if (byCode != null && !byCode.getId().equals(exist.getId()))
            {
                throw new ServiceException("栏目编码已存在: " + form.getCode());
            }
        }

        if (form.getParentId() != null && !form.getParentId().equals(exist.getParentId()))
        {
            assertNoCycle(form.getId(), form.getParentId());
        }

        form.setUpdateBy(SecurityUtils.getUsername());
        channelMapper.updateById(form);
        return getById(form.getId());
    }

    @Transactional
    public void delete(Long id)
    {
        Channel exist = getById(id);
        if (channelMapper.countByParentId(id) > 0)
        {
            throw new ServiceException("栏目下还有子栏目，不能删除: " + exist.getName());
        }
        if (articleMapper.countByChannelId(id) > 0)
        {
            throw new ServiceException("栏目下还有文章，不能删除: " + exist.getName());
        }
        channelMapper.softDelete(id, SecurityUtils.getUsername());
    }

    private static void validateCommon(Channel form, Long selfId)
    {
        if (StringUtils.isEmpty(form.getCode())) throw new ServiceException("栏目 code 不能为空");
        if (StringUtils.isEmpty(form.getName())) throw new ServiceException("栏目名称不能为空");
        if (form.getParentId() != null && form.getParentId() < 0)
        {
            throw new ServiceException("parentId 非法");
        }
        if (selfId != null && form.getParentId() != null && form.getParentId().equals(selfId))
        {
            throw new ServiceException("不能把栏目设为自己的子节点");
        }
    }

    /** 防止把 self 挂到自己的子孙下；遍历 newParent 一路向上看是否会经过 self。 */
    private void assertNoCycle(Long selfId, Long newParentId)
    {
        if (newParentId == null || newParentId == 0L) return;
        Long cur = newParentId;
        int hops = 0;
        while (cur != null && cur != 0L && hops++ < 32)
        {
            if (cur.equals(selfId))
            {
                throw new ServiceException("循环引用：不能把栏目挂到自己的子孙下");
            }
            Channel up = channelMapper.selectById(cur);
            if (up == null) break;
            cur = up.getParentId();
        }
    }

    static List<ChannelTreeNode> buildTree(List<Channel> rows)
    {
        Map<Long, ChannelTreeNode> idx = new HashMap<>();
        List<ChannelTreeNode> roots = new ArrayList<>();
        for (Channel c : rows)
        {
            ChannelTreeNode n = ChannelTreeNode.of(c);
            idx.put(n.getId(), n);
        }
        for (ChannelTreeNode n : idx.values())
        {
            Long pid = n.getParentId();
            if (pid == null || pid == 0L || !idx.containsKey(pid))
            {
                roots.add(n);
            }
            else
            {
                idx.get(pid).getChildren().add(n);
            }
        }
        return roots;
    }
}
