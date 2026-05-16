package com.scaffold.module.report.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.scaffold.module.report.domain.SysReportDataSource;

public interface SysReportDataSourceMapper
{
    List<SysReportDataSource> selectAll();

    SysReportDataSource selectById(@Param("id") Long id);

    SysReportDataSource selectByCode(@Param("code") String code);

    int insert(SysReportDataSource ds);

    int updateById(SysReportDataSource ds);

    int deleteById(@Param("id") Long id);
}
