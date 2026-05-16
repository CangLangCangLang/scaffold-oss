package com.scaffold.module.report.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.scaffold.module.report.domain.SysReportDashboard;

public interface SysReportDashboardMapper
{
    List<SysReportDashboard> selectPage(@Param("name") String name,
                                        @Param("category") String category,
                                        @Param("status") String status,
                                        @Param("offset") int offset,
                                        @Param("limit") int limit);

    long count(@Param("name") String name,
               @Param("category") String category,
               @Param("status") String status);

    SysReportDashboard selectById(@Param("id") Long id);

    SysReportDashboard selectByCode(@Param("code") String code);

    int insert(SysReportDashboard d);

    int updateById(SysReportDashboard d);

    int deleteById(@Param("id") Long id);
}
