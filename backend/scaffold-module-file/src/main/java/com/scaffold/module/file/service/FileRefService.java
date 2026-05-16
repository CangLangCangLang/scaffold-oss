package com.scaffold.module.file.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.scaffold.common.utils.SecurityUtils;
import com.scaffold.module.file.domain.SysFileRef;
import com.scaffold.module.file.mapper.SysFileMapper;
import com.scaffold.module.file.mapper.SysFileRefMapper;

/**
 * 跨模块引用 API（CMS / Form / Workflow 等模块对外引用本中心文件的句柄）。
 *
 * <p>设计目的：保证文件被业务系统挂用时不会被意外删掉。任何业务模块在持有文件链接时调
 * {@link #attach(Long, String, String, String)}，解除时调 {@link #detach(Long, String, String, String)}。
 *
 * <p>本服务公开为 Spring bean，CMS / Form 等其它模块只需 {@code @Autowired} 即可使用 — 没有就不调，零耦合。
 *
 * @author scaffold
 */
@Service
public class FileRefService
{
    @Autowired private SysFileRefMapper refMapper;
    @Autowired private SysFileMapper fileMapper;

    /**
     * 建立一条引用：sys_file_ref INSERT IGNORE + sys_file.ref_count += 1（仅在首次插入成功时）。
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean attach(Long fileId, String module, String type, String id)
    {
        SysFileRef r = new SysFileRef();
        r.setFileId(fileId);
        r.setRefModule(module);
        r.setRefType(type);
        r.setRefId(id);
        r.setCreateBy(SecurityUtils.getUsername());
        int rows = refMapper.insertIgnore(r);
        if (rows > 0)
        {
            fileMapper.incrRefCount(fileId);
        }
        return rows > 0;
    }

    /**
     * 解除一条引用：sys_file_ref DELETE + sys_file.ref_count -= 1（仅在删除成功时）。
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean detach(Long fileId, String module, String type, String id)
    {
        int rows = refMapper.deleteOne(fileId, module, type, id);
        if (rows > 0)
        {
            fileMapper.decrRefCount(fileId);
        }
        return rows > 0;
    }

    public List<SysFileRef> listByFile(Long fileId)
    {
        return refMapper.selectByFile(fileId);
    }
}
