package com.scaffold.system.mapper;

import org.apache.ibatis.annotations.Param;
import com.scaffold.system.domain.SysUserExternalIdentity;

/**
 * 外部身份绑定 Mapper。
 *
 * @author scaffold
 */
public interface SysUserExternalIdentityMapper
{
    SysUserExternalIdentity selectByProviderAndSubject(@Param("provider") String provider,
                                                       @Param("subject") String subject);

    int insert(SysUserExternalIdentity identity);

    int updateLastLoginAt(@Param("id") Long id);
}
