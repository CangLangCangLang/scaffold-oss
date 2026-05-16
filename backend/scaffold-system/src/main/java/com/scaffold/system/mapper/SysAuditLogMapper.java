package com.scaffold.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.scaffold.system.domain.SysAuditLog;

/**
 * 操作审计 Mapper。
 *
 * @author scaffold
 */
public interface SysAuditLogMapper
{
    int insert(SysAuditLog record);

    SysAuditLog selectById(@Param("id") Long id);

    /**
     * 多条件检索（任意条件均可为空）。第一参数必须是 {@link SysAuditLog}（继承自 BaseEntity）：
     * {@link com.scaffold.framework.aspectj.DataScopeAspect} 会把生成的过滤 SQL 写到
     * {@code params.dataScope} 中，由 mapper.xml 里 <code>${params.dataScope}</code> 占位拼接。
     */
    List<SysAuditLog> selectList(SysAuditLog query);

    /** 物理删除指定天数前的记录（保留近 N 天） */
    int deleteOlderThan(@Param("days") int days);

    long countOlderThan(@Param("days") int days);
}
