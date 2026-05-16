package com.scaffold.module.report.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.scaffold.module.report.domain.SysReportDashboardCard;

public interface SysReportDashboardCardMapper
{
    List<SysReportDashboardCard> selectByDashboardId(@Param("dashboardId") Long dashboardId);

    SysReportDashboardCard selectById(@Param("id") Long id);

    int insert(SysReportDashboardCard c);

    int updateById(SysReportDashboardCard c);

    int deleteById(@Param("id") Long id);

    int deleteByDashboardId(@Param("dashboardId") Long dashboardId);
}
