package com.scaffold.module.cms.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.scaffold.module.cms.domain.Channel;

/**
 * 栏目 mapper。所有 select 默认强制 {@code del_flag='0'} 过滤。
 */
public interface ChannelMapper
{
    /** 列表（树查询前置；按 parent_id + order_num 全量返回，前端拼树）。 */
    List<Channel> selectList(Channel filter);

    /** 仅返回未停用的栏目（公开 API 用）。 */
    List<Channel> selectActiveList();

    Channel selectById(@Param("id") Long id);

    Channel selectByCode(@Param("code") String code);

    int countByParentId(@Param("parentId") Long parentId);

    int insert(Channel channel);

    int updateById(Channel channel);

    /** 软删（del_flag=2）。子栏目存在时 service 层会先校验。 */
    int softDelete(@Param("id") Long id, @Param("updateBy") String updateBy);
}
