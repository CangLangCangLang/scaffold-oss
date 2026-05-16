package com.scaffold.module.report.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.scaffold.common.exception.ServiceException;
import com.scaffold.common.utils.SecurityUtils;
import com.scaffold.module.report.domain.SysReportDataSource;
import com.scaffold.module.report.dto.DataSourceUpsertRequest;
import com.scaffold.module.report.mapper.SysReportDataSourceMapper;
import com.scaffold.module.report.runtime.ReportDataSourceManager;

/**
 * 外部数据源 CRUD 与试连。密码字段在保存时由
 * {@link com.scaffold.common.utils.Aes256Util} 加密落库；
 * 编辑时 password=null 视为不动、空串视为清空、非空视为重新加密。
 *
 * @author scaffold
 */
@Service
public class DataSourceService
{
    @Autowired
    private SysReportDataSourceMapper mapper;

    @Autowired
    private ReportDataSourceManager dataSourceManager;

    public List<SysReportDataSource> list()
    {
        List<SysReportDataSource> all = mapper.selectAll();
        for (SysReportDataSource ds : all)
        {
            ds.setPasswordMask(maskOf(ds.getPasswordEnc()));
            ds.setPasswordEnc(null); // 不让密文外泄
        }
        return all;
    }

    public SysReportDataSource detail(Long id)
    {
        SysReportDataSource ds = mapper.selectById(id);
        if (ds == null) throw new ServiceException("数据源不存在");
        ds.setPasswordMask(maskOf(ds.getPasswordEnc()));
        ds.setPasswordEnc(null);
        return ds;
    }

    @Transactional
    public Long save(DataSourceUpsertRequest req)
    {
        if (req == null) throw new ServiceException("参数缺失");
        SysReportDataSource entity;
        if (req.getId() == null)
        {
            validate(req);
            if (mapper.selectByCode(req.getCode()) != null)
            {
                throw new ServiceException("数据源编码已存在：" + req.getCode());
            }
            entity = new SysReportDataSource();
            entity.setCode(req.getCode());
            entity.setName(req.getName());
            entity.setType(req.getType() == null ? "mysql" : req.getType());
            entity.setJdbcUrl(req.getJdbcUrl());
            entity.setDriverClass(req.getDriverClass());
            entity.setUsername(req.getUsername());
            entity.setPasswordEnc(encryptIfPresent(req.getPassword(), dataSourceManager));
            entity.setStatus(req.getStatus() == null ? "0" : req.getStatus());
            entity.setRemark(req.getRemark());
            entity.setCreateBy(SecurityUtils.getUsername());
            mapper.insert(entity);
            return entity.getId();
        }
        SysReportDataSource exist = mapper.selectById(req.getId());
        if (exist == null) throw new ServiceException("数据源不存在");
        SysReportDataSource patch = new SysReportDataSource();
        patch.setId(req.getId());
        patch.setName(req.getName());
        patch.setType(req.getType());
        patch.setJdbcUrl(req.getJdbcUrl());
        patch.setDriverClass(req.getDriverClass());
        patch.setUsername(req.getUsername());
        patch.setStatus(req.getStatus());
        patch.setRemark(req.getRemark());
        if (req.getPassword() != null)
        {
            patch.setPasswordEnc(req.getPassword().isEmpty()
                    ? "" : dataSourceManager.encrypt(req.getPassword()));
        }
        patch.setUpdateBy(SecurityUtils.getUsername());
        mapper.updateById(patch);
        dataSourceManager.invalidate(req.getId());
        return req.getId();
    }

    @Transactional
    public void remove(Long id)
    {
        if (id == null || id <= 0) throw new ServiceException("非法 ID");
        if (mapper.deleteById(id) <= 0) throw new ServiceException("数据源不存在或已删除");
        dataSourceManager.invalidate(id);
    }

    /**
     * 试连。非编辑场景（前端只填 password），直接传明文；
     * 编辑场景（已有记录的密码不重填），后端从库里取密文解密回填一次。
     */
    public void test(DataSourceUpsertRequest req)
    {
        SysReportDataSource cfg = new SysReportDataSource();
        cfg.setCode(req.getCode() == null ? "test-" + System.currentTimeMillis() : req.getCode());
        cfg.setType(req.getType() == null ? "mysql" : req.getType());
        cfg.setJdbcUrl(req.getJdbcUrl());
        cfg.setDriverClass(req.getDriverClass());
        cfg.setUsername(req.getUsername());
        cfg.setStatus("0");
        if (req.getPassword() != null && !req.getPassword().isEmpty())
        {
            cfg.setPasswordEnc(dataSourceManager.encrypt(req.getPassword()));
        }
        else if (req.getId() != null)
        {
            SysReportDataSource exist = mapper.selectById(req.getId());
            if (exist != null) cfg.setPasswordEnc(exist.getPasswordEnc());
        }
        dataSourceManager.test(cfg);
    }

    private static String encryptIfPresent(String plain, ReportDataSourceManager mgr)
    {
        return (plain == null || plain.isEmpty()) ? null : mgr.encrypt(plain);
    }

    private static String maskOf(String enc)
    {
        return (enc == null || enc.isEmpty()) ? "" : "********";
    }

    private static void validate(DataSourceUpsertRequest req)
    {
        if (req.getCode() == null || req.getCode().isEmpty()) throw new ServiceException("code 必填");
        if (req.getName() == null || req.getName().isEmpty()) throw new ServiceException("name 必填");
        if (req.getJdbcUrl() == null || req.getJdbcUrl().isEmpty()) throw new ServiceException("jdbcUrl 必填");
    }
}
