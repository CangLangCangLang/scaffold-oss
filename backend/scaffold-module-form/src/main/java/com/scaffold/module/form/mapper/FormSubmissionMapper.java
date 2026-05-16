package com.scaffold.module.form.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.scaffold.module.form.domain.FormSubmission;
import com.scaffold.module.form.dto.FormSubmissionQuery;

public interface FormSubmissionMapper
{
    int insert(FormSubmission s);

    FormSubmission selectById(@Param("id") Long id);

    /** 分页查询提交记录（按 submitter / template / 时间区间过滤） */
    List<FormSubmission> selectPage(@Param("q") FormSubmissionQuery q,
                                    @Param("offset") int offset,
                                    @Param("limit") int limit);

    long count(@Param("q") FormSubmissionQuery q);
}
