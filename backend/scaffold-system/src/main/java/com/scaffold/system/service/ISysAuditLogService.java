package com.scaffold.system.service;

import com.scaffold.system.domain.SysAuditLog;
import java.util.List;

/**
 * 操作审计 Service。
 *
 * @author scaffold
 */
public interface ISysAuditLogService
{
    void record(SysAuditLog record);

    SysAuditLog selectById(Long id);

    /**
     * 多条件检索 + 数据级权限过滤。
     * 调用方在 controller 上挂 {@code @DataScope(deptAlias="d", userAlias="u")}，
     * AOP 会把过滤 SQL 注入到 {@code query.params.dataScope}，mapper.xml 拼接到 where 末尾。
     */
    List<SysAuditLog> selectList(SysAuditLog query);

    int deleteOlderThan(int days);

    long countOlderThan(int days);
}
