package com.scaffold.module.form.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import com.scaffold.common.core.domain.entity.SysUser;
import com.scaffold.common.core.domain.model.LoginUser;
import com.scaffold.common.exception.ServiceException;
import com.scaffold.module.form.domain.FormSubmission;
import com.scaffold.module.form.domain.FormTemplate;
import com.scaffold.module.form.dto.FormSubmissionQuery;
import com.scaffold.module.form.dto.FormSubmissionRequest;
import com.scaffold.module.form.mapper.FormSubmissionMapper;

/**
 * FormSubmissionService 单测：覆盖提交校验 / 模板状态门 / 横向越权防线 / admin 全量 vs
 * 普通用户自隔离。
 */
class FormSubmissionServiceTest
{
    private FormSubmissionMapper submissionMapper;
    private FormTemplateService templateService;
    private FormSubmissionService submissionService;

    @BeforeEach
    void setUp()
    {
        submissionMapper = mock(FormSubmissionMapper.class);
        templateService = mock(FormTemplateService.class);
        submissionService = new FormSubmissionService();
        ReflectionTestUtils.setField(submissionService, "submissionMapper", submissionMapper);
        ReflectionTestUtils.setField(submissionService, "templateService", templateService);

        loginAs(2L, "alice");
    }

    @AfterEach
    void tearDown()
    {
        SecurityContextHolder.clearContext();
    }

    /* ===== 提交 ===== */

    @Test
    void submitSuccess()
    {
        FormTemplate t = template(10L, "k", "PUBLISHED", 3);
        when(templateService.detail(10L)).thenReturn(t);

        FormSubmissionRequest req = new FormSubmissionRequest();
        req.setTemplateId(10L);
        req.setData("{\"name\":\"alice\"}");
        FormSubmission s = submissionService.submit(req);

        assertThat(s.getTemplateKey()).isEqualTo("k");
        assertThat(s.getTemplateVersion()).isEqualTo(3);
        assertThat(s.getSubmitter()).isEqualTo("alice");
        assertThat(s.getStatus()).isEqualTo("SUBMITTED");
        verify(submissionMapper).insert(any(FormSubmission.class));
    }

    @Test
    void submitRejectsBlankFields()
    {
        FormSubmissionRequest req = new FormSubmissionRequest();
        assertThatThrownBy(() -> submissionService.submit(req))
                .isInstanceOf(ServiceException.class).hasMessageContaining("templateId");

        req.setTemplateId(1L);
        assertThatThrownBy(() -> submissionService.submit(req))
                .isInstanceOf(ServiceException.class).hasMessageContaining("data");
    }

    @Test
    void submitRejectsBadJsonData()
    {
        FormSubmissionRequest req = new FormSubmissionRequest();
        req.setTemplateId(1L);
        req.setData("not-json");
        assertThatThrownBy(() -> submissionService.submit(req))
                .isInstanceOf(ServiceException.class).hasMessageContaining("JSON");
    }

    @Test
    void submitRejectsArrayJsonData()
    {
        FormSubmissionRequest req = new FormSubmissionRequest();
        req.setTemplateId(1L);
        req.setData("[1,2]");
        assertThatThrownBy(() -> submissionService.submit(req))
                .isInstanceOf(ServiceException.class).hasMessageContaining("JSON 对象");
    }

    @Test
    void submitRejectsDraftTemplate()
    {
        FormTemplate t = template(11L, "k", "DRAFT", 1);
        when(templateService.detail(11L)).thenReturn(t);

        FormSubmissionRequest req = new FormSubmissionRequest();
        req.setTemplateId(11L);
        req.setData("{}");
        assertThatThrownBy(() -> submissionService.submit(req))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("已发布");
    }

    /* ===== 详情：横向越权 ===== */

    @Test
    void detailAllowsOwner()
    {
        FormSubmission s = submission(20L, "alice", "k");
        when(submissionMapper.selectById(20L)).thenReturn(s);
        FormSubmission ret = submissionService.detail(20L);
        assertThat(ret).isSameAs(s);
    }

    @Test
    void detailRejectsCrossUser()
    {
        FormSubmission s = submission(21L, "bob", "k");
        when(submissionMapper.selectById(21L)).thenReturn(s);
        assertThatThrownBy(() -> submissionService.detail(21L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("无权");
    }

    @Test
    void detailAllowsAdminCrossUser()
    {
        loginAs(1L, "admin");
        FormSubmission s = submission(22L, "bob", "k");
        when(submissionMapper.selectById(22L)).thenReturn(s);
        FormSubmission ret = submissionService.detail(22L);
        assertThat(ret).isSameAs(s);
    }

    /* ===== 列表：admin 全量 vs 普通用户自隔离 ===== */

    @Test
    void pageNonAdminForcedToCurrentUser()
    {
        when(submissionMapper.selectPage(any(), eq(0), eq(20))).thenReturn(List.of());
        when(submissionMapper.count(any())).thenReturn(0L);

        FormSubmissionQuery q = new FormSubmissionQuery();
        q.setSubmitter("bob"); // 想偷偷看 bob 的，应被强制覆盖
        Map<String, Object> ret = submissionService.page(q);
        assertThat(ret).containsKeys("rows", "total");

        org.mockito.ArgumentCaptor<FormSubmissionQuery> cap =
                org.mockito.ArgumentCaptor.forClass(FormSubmissionQuery.class);
        verify(submissionMapper).selectPage(cap.capture(), eq(0), eq(20));
        assertThat(cap.getValue().getSubmitter()).isEqualTo("alice");
    }

    @Test
    void pageAdminKeepsExplicitSubmitter()
    {
        loginAs(1L, "admin");
        when(submissionMapper.selectPage(any(), eq(0), eq(20))).thenReturn(List.of());
        when(submissionMapper.count(any())).thenReturn(0L);

        FormSubmissionQuery q = new FormSubmissionQuery();
        q.setSubmitter("bob");
        submissionService.page(q);

        org.mockito.ArgumentCaptor<FormSubmissionQuery> cap =
                org.mockito.ArgumentCaptor.forClass(FormSubmissionQuery.class);
        verify(submissionMapper).selectPage(cap.capture(), eq(0), eq(20));
        assertThat(cap.getValue().getSubmitter()).isEqualTo("bob");
    }

    /* ===== helpers ===== */

    private static void loginAs(Long uid, String name)
    {
        SecurityContextHolder.clearContext();
        SysUser sys = new SysUser();
        sys.setUserId(uid);
        sys.setUserName(name);
        LoginUser u = new LoginUser(uid, 1L, sys, null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(u, null, Collections.emptyList()));
    }

    private static FormTemplate template(Long id, String key, String status, int version)
    {
        FormTemplate t = new FormTemplate();
        t.setId(id);
        t.setFormKey(key);
        t.setStatus(status);
        t.setVersion(version);
        t.setName("n");
        t.setSchemaJson("[]");
        return t;
    }

    private static FormSubmission submission(Long id, String submitter, String key)
    {
        FormSubmission s = new FormSubmission();
        s.setId(id);
        s.setSubmitter(submitter);
        s.setTemplateKey(key);
        s.setTemplateVersion(1);
        return s;
    }
}
