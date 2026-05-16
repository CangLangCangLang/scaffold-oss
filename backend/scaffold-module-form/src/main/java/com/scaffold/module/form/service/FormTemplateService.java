package com.scaffold.module.form.service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.scaffold.common.exception.ServiceException;
import com.scaffold.common.utils.SecurityUtils;
import com.scaffold.module.form.domain.FormTemplate;
import com.scaffold.module.form.dto.FormTemplateQuery;
import com.scaffold.module.form.dto.FormTemplateSaveRequest;
import com.scaffold.module.form.mapper.FormTemplateMapper;

/**
 * 表单模板业务。
 *
 * <h3>状态机</h3>
 * <pre>
 *   create  → DRAFT
 *   publish → DRAFT  → PUBLISHED  （首发记 publishedAt）
 *   archive → PUBLISHED → ARCHIVED
 *   resave from PUBLISHED → 自动新建 version+1 的 DRAFT，不破坏在线版本
 * </pre>
 *
 * <h3>不变性</h3>
 * formKey 一旦确定不可改：保留与历史 form_submission 中 templateKey 的可链接性。
 *
 * @author scaffold
 */
@Service
public class FormTemplateService
{
    public static final int MAX_PAGE_SIZE = 200;

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_ARCHIVED = "ARCHIVED";

    @Autowired private FormTemplateMapper templateMapper;

    /**
     * 保存模板（新增 / 编辑共用）。
     * <ul>
     *   <li>新增：requireFormKey；同 key 已存在则抛冲突</li>
     *   <li>编辑 DRAFT：原地修改 schemaJson / name 等</li>
     *   <li>编辑 PUBLISHED：自动创建 version+1 的新 DRAFT，不修改在线版本</li>
     * </ul>
     */
    @Transactional
    public FormTemplate save(FormTemplateSaveRequest req)
    {
        if (req == null) throw new ServiceException("请求不能为空");
        if (req.getName() == null || req.getName().isBlank()) throw new ServiceException("name 不能为空");
        if (req.getSchemaJson() == null || req.getSchemaJson().isBlank())
        {
            throw new ServiceException("schemaJson 不能为空");
        }
        validateSchemaJson(req.getSchemaJson());

        String operator = currentUsername();

        if (req.getId() == null)
        {
            // 新增
            if (req.getFormKey() == null || req.getFormKey().isBlank())
            {
                throw new ServiceException("formKey 不能为空");
            }
            FormTemplate exists = templateMapper.selectLatestByFormKey(req.getFormKey());
            if (exists != null) throw new ServiceException("formKey 已存在: " + req.getFormKey());

            FormTemplate t = new FormTemplate();
            t.setFormKey(req.getFormKey().trim());
            t.setName(req.getName());
            t.setCategory(req.getCategory());
            t.setSchemaJson(req.getSchemaJson());
            t.setVersion(1);
            t.setStatus(STATUS_DRAFT);
            t.setDescription(req.getDescription());
            t.setCreateBy(operator);
            t.setUpdateBy(operator);
            templateMapper.insert(t);
            return t;
        }

        FormTemplate cur = templateMapper.selectById(req.getId());
        if (cur == null) throw new ServiceException("模板不存在: " + req.getId());

        if (STATUS_PUBLISHED.equals(cur.getStatus()) || STATUS_ARCHIVED.equals(cur.getStatus()))
        {
            // 已发布 / 归档：派生新版本草稿
            FormTemplate latest = templateMapper.selectLatestByFormKey(cur.getFormKey());
            int nextVersion = (latest == null ? cur.getVersion() : latest.getVersion()) + 1;
            FormTemplate nv = new FormTemplate();
            nv.setFormKey(cur.getFormKey());
            nv.setName(req.getName());
            nv.setCategory(req.getCategory());
            nv.setSchemaJson(req.getSchemaJson());
            nv.setVersion(nextVersion);
            nv.setStatus(STATUS_DRAFT);
            nv.setDescription(req.getDescription());
            nv.setCreateBy(operator);
            nv.setUpdateBy(operator);
            templateMapper.insert(nv);
            return nv;
        }

        // 草稿原地改
        cur.setName(req.getName());
        cur.setCategory(req.getCategory());
        cur.setSchemaJson(req.getSchemaJson());
        cur.setDescription(req.getDescription());
        cur.setUpdateBy(operator);
        templateMapper.updateById(cur);
        return cur;
    }

    /** 发布：DRAFT → PUBLISHED；其它状态原地不动并抛错。 */
    @Transactional
    public FormTemplate publish(Long id)
    {
        FormTemplate t = requireById(id);
        if (!STATUS_DRAFT.equals(t.getStatus()))
        {
            throw new ServiceException("仅草稿状态可发布，当前: " + t.getStatus());
        }
        // 同 formKey 下其它 PUBLISHED 自动归档（保证只有一个生效版本）
        List<FormTemplate> all = templateMapper.selectAllByFormKey(t.getFormKey());
        for (FormTemplate other : all)
        {
            if (other.getId().equals(id)) continue;
            if (STATUS_PUBLISHED.equals(other.getStatus()))
            {
                other.setStatus(STATUS_ARCHIVED);
                other.setUpdateBy(currentUsername());
                templateMapper.updateById(other);
            }
        }
        t.setStatus(STATUS_PUBLISHED);
        if (t.getPublishedAt() == null) t.setPublishedAt(new Date());
        t.setUpdateBy(currentUsername());
        templateMapper.updateById(t);
        return t;
    }

    /** 归档：PUBLISHED → ARCHIVED */
    @Transactional
    public FormTemplate archive(Long id)
    {
        FormTemplate t = requireById(id);
        if (!STATUS_PUBLISHED.equals(t.getStatus()))
        {
            throw new ServiceException("仅已发布可归档，当前: " + t.getStatus());
        }
        t.setStatus(STATUS_ARCHIVED);
        t.setUpdateBy(currentUsername());
        templateMapper.updateById(t);
        return t;
    }

    /** 软删：草稿 / 归档可删；已发布需先归档。 */
    @Transactional
    public void remove(Long id)
    {
        FormTemplate t = requireById(id);
        if (STATUS_PUBLISHED.equals(t.getStatus()))
        {
            throw new ServiceException("已发布的模板请先归档再删除");
        }
        templateMapper.softDeleteById(id, currentUsername());
    }

    public FormTemplate detail(Long id)
    {
        return requireById(id);
    }

    /** 给前端"填报"页：直接返当前 formKey 下唯一 PUBLISHED 版本。 */
    public FormTemplate activeByKey(String formKey)
    {
        if (formKey == null || formKey.isBlank())
        {
            throw new ServiceException("formKey 不能为空");
        }
        List<FormTemplate> all = templateMapper.selectAllByFormKey(formKey);
        for (FormTemplate t : all)
        {
            if (STATUS_PUBLISHED.equals(t.getStatus())) return t;
        }
        return null;
    }

    public Map<String, Object> page(FormTemplateQuery q)
    {
        if (q == null) q = new FormTemplateQuery();
        int pageNum = q.getPageNum() == null || q.getPageNum() < 1 ? 1 : q.getPageNum();
        int pageSize = q.getPageSize() == null || q.getPageSize() < 1 ? 20 : Math.min(q.getPageSize(), MAX_PAGE_SIZE);
        int offset = (pageNum - 1) * pageSize;
        List<FormTemplate> rows = templateMapper.selectPage(q, offset, pageSize);
        long total = templateMapper.count(q);
        Map<String, Object> ret = new HashMap<>(2);
        ret.put("rows", rows);
        ret.put("total", total);
        return ret;
    }

    private FormTemplate requireById(Long id)
    {
        if (id == null) throw new ServiceException("id 不能为空");
        FormTemplate t = templateMapper.selectById(id);
        if (t == null) throw new ServiceException("模板不存在: " + id);
        return t;
    }

    /**
     * schemaJson 校验：仅检查能解析为合法 JSON 数组（form-create rule[]）；
     * 字段级业务校验留给前端 designer 输出（已经在 form-create 内做了）。
     */
    private void validateSchemaJson(String json)
    {
        try
        {
            String trimmed = json.trim();
            if (!trimmed.startsWith("["))
            {
                throw new ServiceException("schemaJson 必须是 JSON 数组（form-create rule[]）");
            }
            // Jackson 可在 framework 层引入，这里用最小校验避免新增依赖
            new com.fasterxml.jackson.databind.ObjectMapper().readTree(trimmed);
        }
        catch (com.fasterxml.jackson.core.JsonProcessingException e)
        {
            throw new ServiceException("schemaJson 解析失败: " + e.getOriginalMessage());
        }
    }

    private String currentUsername()
    {
        try
        {
            return SecurityUtils.getUsername();
        }
        catch (Exception e)
        {
            return "system";
        }
    }
}
