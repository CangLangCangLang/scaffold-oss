package com.scaffold.module.workflow.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.scaffold.common.exception.ServiceException;
import com.scaffold.module.workflow.domain.WfFormSchema;
import com.scaffold.module.workflow.mapper.WfFormSchemaMapper;

/**
 * 工作流动态表单 schema 服务：CRUD + 版本递增 + 启用切换。
 *
 * @author scaffold
 */
@Service
public class WfFormSchemaService
{
    @Autowired
    private WfFormSchemaMapper formSchemaMapper;

    /**
     * 保存或新建版本：每次保存都新增一行版本记录，并把同 (key, activityId) 下其他行设为 enabled=0。
     * 这样可以做到"启用最新版本但保留历史快照"。
     */
    @Transactional
    public WfFormSchema saveAsNewVersion(WfFormSchema entry, String operator)
    {
        if (entry.getProcessDefinitionKey() == null || entry.getProcessDefinitionKey().isBlank())
        {
            throw new ServiceException("processDefinitionKey 不能为空");
        }
        if (entry.getSchemaJson() == null || entry.getSchemaJson().isBlank())
        {
            throw new ServiceException("schemaJson 不能为空");
        }
        if (entry.getActivityId() == null || entry.getActivityId().isBlank())
        {
            entry.setActivityId(WfFormSchema.ACTIVITY_START_FORM);
        }

        WfFormSchema cur = formSchemaMapper.selectActiveLatest(
                entry.getProcessDefinitionKey(), entry.getActivityId());
        int nextVersion = cur == null ? 1 : (cur.getVersion() == null ? 1 : cur.getVersion() + 1);
        entry.setVersion(nextVersion);
        entry.setEnabled(true);
        entry.setCreateBy(operator);
        entry.setUpdateBy(operator);
        formSchemaMapper.insert(entry);
        // 把同 key+activityId 下其他启用的版本设为 disabled，确保 selectActiveLatest 唯一
        formSchemaMapper.disableOldVersions(
                entry.getProcessDefinitionKey(), entry.getActivityId(), entry.getId());
        return entry;
    }

    /** 获取流程定义 key + activityId 下当前生效的 schema；找不到返回 null（不抛异常，由调用方决定降级）。 */
    public WfFormSchema findActive(String processDefinitionKey, String activityId)
    {
        if (processDefinitionKey == null || processDefinitionKey.isBlank()) return null;
        String aid = (activityId == null || activityId.isBlank())
                ? WfFormSchema.ACTIVITY_START_FORM : activityId;
        return formSchemaMapper.selectActiveLatest(processDefinitionKey, aid);
    }

    public WfFormSchema findById(Long id)
    {
        return formSchemaMapper.selectById(id);
    }

    public List<WfFormSchema> listByDefinitionKey(String processDefinitionKey)
    {
        if (processDefinitionKey == null || processDefinitionKey.isBlank()) return List.of();
        return formSchemaMapper.selectAllByDefinitionKey(processDefinitionKey);
    }

    @Transactional
    public int delete(Long id, String operator)
    {
        WfFormSchema cur = formSchemaMapper.selectById(id);
        if (cur == null) return 0;
        return formSchemaMapper.deleteById(id);
    }
}
