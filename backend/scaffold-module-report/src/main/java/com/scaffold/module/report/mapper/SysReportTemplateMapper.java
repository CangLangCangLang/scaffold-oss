package com.scaffold.module.report.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.scaffold.module.report.domain.SysReportTemplate;
import com.scaffold.module.report.dto.TemplateQuery;

public interface SysReportTemplateMapper
{
    List<SysReportTemplate> selectPage(@Param("q") TemplateQuery q,
                                       @Param("offset") int offset,
                                       @Param("limit") int limit);

    long count(@Param("q") TemplateQuery q);

    SysReportTemplate selectById(@Param("id") Long id);

    SysReportTemplate selectByCode(@Param("code") String code);

    int insert(SysReportTemplate t);

    int updateById(SysReportTemplate t);

    int deleteById(@Param("id") Long id);
}
