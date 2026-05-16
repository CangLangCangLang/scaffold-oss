package com.scaffold.module.form.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.scaffold.module.form.domain.FormTemplate;
import com.scaffold.module.form.dto.FormTemplateQuery;

public interface FormTemplateMapper
{
    /** 分页列表（关键字 LIKE / 分类 / 状态 + del_flag=0） */
    List<FormTemplate> selectPage(@Param("q") FormTemplateQuery q,
                                  @Param("offset") int offset,
                                  @Param("limit") int limit);

    /** 总数（与 selectPage 同 where，但忽略分页） */
    long count(@Param("q") FormTemplateQuery q);

    FormTemplate selectById(@Param("id") Long id);

    /** 同 formKey 的最新版本（version DESC，仅看 del_flag=0）；新增时检查 key 冲突用 */
    FormTemplate selectLatestByFormKey(@Param("formKey") String formKey);

    /** 同 formKey 下的所有历史版本（DESC），给前端 版本回看 用 */
    List<FormTemplate> selectAllByFormKey(@Param("formKey") String formKey);

    int insert(FormTemplate t);

    int updateById(FormTemplate t);

    /** 软删（del_flag=2，update_time 自动） */
    int softDeleteById(@Param("id") Long id, @Param("operator") String operator);
}
