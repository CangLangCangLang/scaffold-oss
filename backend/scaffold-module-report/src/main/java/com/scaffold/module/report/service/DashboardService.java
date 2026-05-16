package com.scaffold.module.report.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.scaffold.common.exception.ServiceException;
import com.scaffold.common.utils.SecurityUtils;
import com.scaffold.module.report.domain.SysReportDashboard;
import com.scaffold.module.report.domain.SysReportDashboardCard;
import com.scaffold.module.report.mapper.SysReportDashboardCardMapper;
import com.scaffold.module.report.mapper.SysReportDashboardMapper;

/**
 * 看板与卡片：保存时整批替换卡片（避免 patch 复杂度）。
 *
 * @author scaffold
 */
@Service
public class DashboardService
{
    @Autowired
    private SysReportDashboardMapper dashMapper;

    @Autowired
    private SysReportDashboardCardMapper cardMapper;

    public List<SysReportDashboard> page(String name, String category, String status,
                                         int pageNum, int pageSize)
    {
        int pn = pageNum <= 0 ? 1 : pageNum;
        int ps = pageSize <= 0 ? 10 : Math.min(pageSize, 200);
        return dashMapper.selectPage(name, category, status, (pn - 1) * ps, ps);
    }

    public long total(String name, String category, String status)
    {
        return dashMapper.count(name, category, status);
    }

    public Map<String, Object> detail(Long id)
    {
        SysReportDashboard d = dashMapper.selectById(id);
        if (d == null) throw new ServiceException("看板不存在");
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("dashboard", d);
        r.put("cards", cardMapper.selectByDashboardId(id));
        return r;
    }

    @Transactional
    public Long save(SysReportDashboard d, List<SysReportDashboardCard> cards)
    {
        if (d == null) throw new ServiceException("看板信息缺失");
        Long id = d.getId();
        if (id == null)
        {
            if (d.getCode() == null || d.getCode().isEmpty()) throw new ServiceException("code 必填");
            if (dashMapper.selectByCode(d.getCode()) != null) throw new ServiceException("看板编码已存在");
            d.setStatus(d.getStatus() == null ? "0" : d.getStatus());
            d.setCreateBy(SecurityUtils.getUsername());
            dashMapper.insert(d);
            id = d.getId();
        }
        else
        {
            SysReportDashboard exist = dashMapper.selectById(id);
            if (exist == null) throw new ServiceException("看板不存在");
            d.setCode(null);
            d.setUpdateBy(SecurityUtils.getUsername());
            dashMapper.updateById(d);
        }
        cardMapper.deleteByDashboardId(id);
        if (cards != null)
        {
            for (SysReportDashboardCard c : cards)
            {
                c.setId(null);
                c.setDashboardId(id);
                if (c.getChartType() == null) c.setChartType("table");
                if (c.getPosX() == null) c.setPosX(0);
                if (c.getPosY() == null) c.setPosY(0);
                if (c.getPosW() == null) c.setPosW(6);
                if (c.getPosH() == null) c.setPosH(6);
                if (c.getOrderNum() == null) c.setOrderNum(0);
                cardMapper.insert(c);
            }
        }
        return id;
    }

    @Transactional
    public void remove(Long id)
    {
        cardMapper.deleteByDashboardId(id);
        if (dashMapper.deleteById(id) <= 0) throw new ServiceException("看板不存在");
    }

    public List<SysReportDashboardCard> cardsOf(Long dashboardId)
    {
        if (dashboardId == null) return Collections.emptyList();
        return cardMapper.selectByDashboardId(dashboardId);
    }
}
