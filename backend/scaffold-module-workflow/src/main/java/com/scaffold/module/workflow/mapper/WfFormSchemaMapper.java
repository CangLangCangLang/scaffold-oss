package com.scaffold.module.workflow.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.scaffold.module.workflow.domain.WfFormSchema;

/**
 * 工作流表单 schema Mapper（位于 com.scaffold.**.mapper 包中以匹配全局 MapperScan）。
 *
 * @author scaffold
 */
public interface WfFormSchemaMapper
{
    int insert(WfFormSchema entry);

    int updateSelective(WfFormSchema entry);

    int disableOldVersions(@Param("processDefinitionKey") String processDefinitionKey,
            @Param("activityId") String activityId,
            @Param("excludeId") Long excludeId);

    WfFormSchema selectActiveLatest(@Param("processDefinitionKey") String processDefinitionKey,
            @Param("activityId") String activityId);

    WfFormSchema selectById(@Param("id") Long id);

    List<WfFormSchema> selectAllByDefinitionKey(@Param("processDefinitionKey") String processDefinitionKey);

    int deleteById(@Param("id") Long id);
}
