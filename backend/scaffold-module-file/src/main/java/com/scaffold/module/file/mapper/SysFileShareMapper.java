package com.scaffold.module.file.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.scaffold.module.file.domain.SysFileShare;

public interface SysFileShareMapper
{
    /** 当前用户创建的全部分享（按 createTime DESC） */
    List<SysFileShare> selectByCreator(@Param("createBy") String createBy);

    SysFileShare selectByToken(@Param("token") String token);

    SysFileShare selectById(@Param("id") Long id);

    int insert(SysFileShare s);

    /** visits +1（用作访问累计；被 ShareService.access 调用） */
    int incrVisits(@Param("id") Long id);

    /** 状态变更（停用 / 标记一次性已用尽） */
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    int deleteById(@Param("id") Long id);
}
