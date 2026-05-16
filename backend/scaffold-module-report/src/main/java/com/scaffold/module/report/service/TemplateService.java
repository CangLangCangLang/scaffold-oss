package com.scaffold.module.report.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.scaffold.common.core.page.PageDomain;
import com.scaffold.common.core.page.TableSupport;
import com.scaffold.common.exception.ServiceException;
import com.scaffold.common.utils.SecurityUtils;
import com.scaffold.module.report.domain.SysReportTemplate;
import com.scaffold.module.report.dto.TemplateQuery;
import com.scaffold.module.report.mapper.SysReportTemplateMapper;
import com.scaffold.module.report.runtime.ReportRunner;
import com.scaffold.module.report.security.ReportSqlGuard;

/**
 * 报表模板 CRUD。SQL 在保存时会强制走 SqlGuard 校验，避免向库里写入只读外的语句。
 *
 * @author scaffold
 */
@Service
public class TemplateService
{
    @Autowired
    private SysReportTemplateMapper mapper;

    @Autowired
    private ReportRunner runner;

    public List<SysReportTemplate> page(TemplateQuery q)
    {
        PageDomain p = TableSupport.buildPageRequest();
        Integer pn = p.getPageNum();
        Integer ps = p.getPageSize();
        int pageNum = pn == null || pn <= 0 ? 1 : pn;
        int pageSize = ps == null || ps <= 0 ? 10 : Math.min(ps, 200);
        int offset = (pageNum - 1) * pageSize;
        return mapper.selectPage(q, offset, pageSize);
    }

    public long total(TemplateQuery q)
    {
        return mapper.count(q);
    }

    public SysReportTemplate detail(Long id)
    {
        SysReportTemplate t = mapper.selectById(id);
        if (t == null)
        {
            throw new ServiceException("模板不存在");
        }
        return t;
    }

    public SysReportTemplate detailByCode(String code)
    {
        return mapper.selectByCode(code);
    }

    @Transactional
    public Long save(SysReportTemplate t)
    {
        validateAndDefault(t);
        if (t.getId() == null)
        {
            if (mapper.selectByCode(t.getCode()) != null)
            {
                throw new ServiceException("模板编码已存在：" + t.getCode());
            }
            t.setStatus(t.getStatus() == null ? "0" : t.getStatus());
            t.setCreateBy(SecurityUtils.getUsername());
            mapper.insert(t);
            return t.getId();
        }
        SysReportTemplate exist = mapper.selectById(t.getId());
        if (exist == null)
        {
            throw new ServiceException("模板不存在");
        }
        // code 不允许改（避免影响已经引用此 code 的看板）
        t.setCode(null);
        t.setUpdateBy(SecurityUtils.getUsername());
        mapper.updateById(t);
        return t.getId();
    }

    @Transactional
    public void remove(Long id)
    {
        if (mapper.deleteById(id) <= 0)
        {
            throw new ServiceException("模板不存在或已被删除");
        }
    }

    /** 公开校验：校验模板的 sql_text、行数 / 超时限制是否在全局上限内 */
    public void validateAndDefault(SysReportTemplate t)
    {
        if (t.getSqlText() == null || t.getSqlText().trim().isEmpty())
        {
            throw new ServiceException("SQL 不能为空");
        }
        ReportSqlGuard.ensureSelectOnly(t.getSqlText());
        if (t.getRowLimit() == null || t.getRowLimit() <= 0)
        {
            t.setRowLimit(runner.globalRowLimit());
        }
        else if (t.getRowLimit() > runner.globalRowLimit())
        {
            throw new ServiceException("rowLimit 超过全局上限：" + runner.globalRowLimit());
        }
        if (t.getTimeoutMs() == null || t.getTimeoutMs() <= 0)
        {
            t.setTimeoutMs(runner.globalTimeoutMs());
        }
        else if (t.getTimeoutMs() > runner.globalTimeoutMs())
        {
            throw new ServiceException("timeoutMs 超过全局上限：" + runner.globalTimeoutMs());
        }
        if (t.getDatasourceId() == null) t.setDatasourceId(0L);
    }
}
