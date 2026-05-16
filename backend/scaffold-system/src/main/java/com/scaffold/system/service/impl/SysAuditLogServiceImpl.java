package com.scaffold.system.service.impl;

import java.util.Date;
import java.util.List;
// Date 仍用于 record() 中默认填充 createdAt
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.scaffold.common.annotation.DataScope;
import com.scaffold.system.domain.SysAuditLog;
import com.scaffold.system.mapper.SysAuditLogMapper;
import com.scaffold.system.service.ISysAuditLogService;

/**
 * @author scaffold
 */
@Service
public class SysAuditLogServiceImpl implements ISysAuditLogService
{
    private static final Logger log = LoggerFactory.getLogger(SysAuditLogServiceImpl.class);

    @Autowired
    private SysAuditLogMapper mapper;

    @Override
    public void record(SysAuditLog record)
    {
        if (record == null) return;
        try
        {
            if (record.getCreatedAt() == null) record.setCreatedAt(new Date());
            mapper.insert(record);
        }
        catch (Exception ex)
        {
            // 审计落库失败不可阻断业务，只 warn
            log.warn("审计日志落库失败 module={} action={} resource={}/{} reason={}",
                    record.getModule(), record.getAction(),
                    record.getResourceType(), record.getResourceId(), ex.getMessage());
        }
    }

    @Override
    public SysAuditLog selectById(Long id) { return mapper.selectById(id); }

    /**
     * 数据级权限示例：在 service 上挂 {@link DataScope}，
     * AOP 会从入参（继承自 BaseEntity 的 {@link SysAuditLog}）的 params 写入过滤 SQL，
     * 由 SysAuditLogMapper.xml 通过 <code>${params.dataScope}</code> 拼到 where 末尾。
     * <br>
     * 别名约定：
     * <ul>
     *   <li>{@code d}：sys_dept 表，对应 {@code a.actor_dept_id}</li>
     *   <li>{@code u}：sys_user 表，对应 {@code a.actor_id}（仅本人范围生效）</li>
     * </ul>
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public List<SysAuditLog> selectList(SysAuditLog query)
    {
        return mapper.selectList(query);
    }

    @Override
    public int deleteOlderThan(int days)
    {
        return mapper.deleteOlderThan(Math.max(1, days));
    }

    @Override
    public long countOlderThan(int days)
    {
        return mapper.countOlderThan(Math.max(1, days));
    }
}
