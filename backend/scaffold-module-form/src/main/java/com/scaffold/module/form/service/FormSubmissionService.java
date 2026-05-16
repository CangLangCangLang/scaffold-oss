package com.scaffold.module.form.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.scaffold.common.exception.ServiceException;
import com.scaffold.common.utils.SecurityUtils;
import com.scaffold.module.form.domain.FormSubmission;
import com.scaffold.module.form.domain.FormTemplate;
import com.scaffold.module.form.dto.FormSubmissionQuery;
import com.scaffold.module.form.dto.FormSubmissionRequest;
import com.scaffold.module.form.mapper.FormSubmissionMapper;

/**
 * 表单提交业务。
 *
 * <h3>提交流程</h3>
 * <ol>
 *   <li>校验目标模板存在且 status=PUBLISHED</li>
 *   <li>校验 data 是合法 JSON 对象</li>
 *   <li>把模板 key + version 冗余落到 form_submission（提交瞬间快照，不受后续模板版本变更影响）</li>
 * </ol>
 *
 * <h3>权限隔离</h3>
 * <ul>
 *   <li>填报：登录即可（鉴权 form:submission:add）</li>
 *   <li>列表：admin 看全量；非 admin 强制按 submitter=current 过滤（service 层在 page() 内 enforce）</li>
 *   <li>详情：admin 任意；非 admin 仅能看自己提交的，否则 403</li>
 * </ul>
 *
 * @author scaffold
 */
@Service
public class FormSubmissionService
{
    public static final int MAX_PAGE_SIZE = 200;
    public static final String STATUS_SUBMITTED = "SUBMITTED";

    @Autowired private FormSubmissionMapper submissionMapper;
    @Autowired private FormTemplateService templateService;

    @Transactional
    public FormSubmission submit(FormSubmissionRequest req)
    {
        if (req == null) throw new ServiceException("请求不能为空");
        if (req.getTemplateId() == null) throw new ServiceException("templateId 不能为空");
        if (req.getData() == null || req.getData().isBlank()) throw new ServiceException("data 不能为空");
        validateDataJson(req.getData());

        FormTemplate template = templateService.detail(req.getTemplateId());
        if (!FormTemplateService.STATUS_PUBLISHED.equals(template.getStatus()))
        {
            throw new ServiceException("仅已发布的模板可填报，当前: " + template.getStatus());
        }

        FormSubmission s = new FormSubmission();
        s.setTemplateId(template.getId());
        s.setTemplateKey(template.getFormKey());
        s.setTemplateVersion(template.getVersion());
        s.setSubmitter(currentUsername());
        s.setSubmitterName(currentNickname());
        s.setStatus(STATUS_SUBMITTED);
        s.setData(req.getData());
        submissionMapper.insert(s);
        return s;
    }

    /** 详情；非 admin 仅能查自己的（拉横向越权防线在 service 层）。 */
    public FormSubmission detail(Long id)
    {
        if (id == null) throw new ServiceException("id 不能为空");
        FormSubmission s = submissionMapper.selectById(id);
        if (s == null) throw new ServiceException("提交记录不存在: " + id);
        if (!isAdmin() && !equalsSafe(s.getSubmitter(), currentUsername()))
        {
            throw new ServiceException("无权查看他人提交记录");
        }
        return s;
    }

    public Map<String, Object> page(FormSubmissionQuery q)
    {
        if (q == null) q = new FormSubmissionQuery();
        // 非 admin 强制覆盖 submitter
        if (!isAdmin()) q.setSubmitter(currentUsername());
        int pageNum = q.getPageNum() == null || q.getPageNum() < 1 ? 1 : q.getPageNum();
        int pageSize = q.getPageSize() == null || q.getPageSize() < 1 ? 20 : Math.min(q.getPageSize(), MAX_PAGE_SIZE);
        int offset = (pageNum - 1) * pageSize;
        List<FormSubmission> rows = submissionMapper.selectPage(q, offset, pageSize);
        long total = submissionMapper.count(q);
        Map<String, Object> ret = new HashMap<>(2);
        ret.put("rows", rows);
        ret.put("total", total);
        return ret;
    }

    private void validateDataJson(String json)
    {
        try
        {
            String trimmed = json.trim();
            if (!trimmed.startsWith("{"))
            {
                throw new ServiceException("data 必须是 JSON 对象");
            }
            new com.fasterxml.jackson.databind.ObjectMapper().readTree(trimmed);
        }
        catch (com.fasterxml.jackson.core.JsonProcessingException e)
        {
            throw new ServiceException("data 解析失败: " + e.getOriginalMessage());
        }
    }

    private static boolean equalsSafe(String a, String b)
    {
        return a == null ? b == null : a.equals(b);
    }

    private boolean isAdmin()
    {
        try
        {
            return SecurityUtils.isAdmin(SecurityUtils.getUserId());
        }
        catch (Exception e)
        {
            return false;
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

    private String currentNickname()
    {
        try
        {
            return SecurityUtils.getLoginUser().getUser().getNickName();
        }
        catch (Exception e)
        {
            return null;
        }
    }
}
