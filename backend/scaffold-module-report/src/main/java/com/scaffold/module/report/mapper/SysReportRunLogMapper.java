package com.scaffold.module.report.mapper;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.scaffold.module.report.domain.SysReportRunLog;

public interface SysReportRunLogMapper
{
    int insert(SysReportRunLog log);

    List<SysReportRunLog> selectPage(@Param("templateId") Long templateId,
                                     @Param("createBy") String createBy,
                                     @Param("status") String status,
                                     @Param("offset") int offset,
                                     @Param("limit") int limit);

    long count(@Param("templateId") Long templateId,
               @Param("createBy") String createBy,
               @Param("status") String status);

    int deleteOlderThan(@Param("threshold") Date threshold);
}
