package com.scaffold.module.form.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import com.scaffold.common.core.domain.entity.SysUser;
import com.scaffold.common.core.domain.model.LoginUser;
import com.scaffold.common.exception.ServiceException;
import com.scaffold.module.form.domain.FormTemplate;
import com.scaffold.module.form.dto.FormTemplateQuery;
import com.scaffold.module.form.dto.FormTemplateSaveRequest;
import com.scaffold.module.form.mapper.FormTemplateMapper;

/**
 * FormTemplateService 单测。
 *
 * <p>覆盖：状态机（DRAFT → PUBLISHED → ARCHIVED）、版本派生、formKey 唯一性、
 * schemaJson 合法性、软删保护、分页 page-size 上限。
 */
class FormTemplateServiceTest
{
    private FormTemplateMapper templateMapper;
    private FormTemplateService templateService;

    @BeforeEach
    void setUp()
    {
        templateMapper = mock(FormTemplateMapper.class);
        templateService = new FormTemplateService();
        ReflectionTestUtils.setField(templateService, "templateMapper", templateMapper);

        SysUser sys = new SysUser();
        sys.setUserId(2L);
        sys.setUserName("designer");
        LoginUser u = new LoginUser(2L, 1L, sys, null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(u, null, Collections.emptyList()));
    }

    @AfterEach
    void tearDown()
    {
        SecurityContextHolder.clearContext();
    }

    /* ===== 创建 ===== */

    @Test
    void createDraftSuccess()
    {
        FormTemplateSaveRequest req = new FormTemplateSaveRequest();
        req.setFormKey("leave_application");
        req.setName("请假申请");
        req.setSchemaJson("[]");

        when(templateMapper.selectLatestByFormKey("leave_application")).thenReturn(null);

        FormTemplate t = templateService.save(req);
        assertThat(t.getStatus()).isEqualTo("DRAFT");
        assertThat(t.getVersion()).isEqualTo(1);
        assertThat(t.getCreateBy()).isEqualTo("designer");
        verify(templateMapper).insert(any(FormTemplate.class));
    }

    @Test
    void createWithDuplicateKeyFails()
    {
        FormTemplate exists = new FormTemplate();
        exists.setId(1L);
        exists.setFormKey("k1");
        when(templateMapper.selectLatestByFormKey("k1")).thenReturn(exists);

        FormTemplateSaveRequest req = new FormTemplateSaveRequest();
        req.setFormKey("k1");
        req.setName("dup");
        req.setSchemaJson("[]");

        assertThatThrownBy(() -> templateService.save(req))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("已存在");
    }

    @Test
    void createRejectsBlankFields()
    {
        FormTemplateSaveRequest req = new FormTemplateSaveRequest();
        assertThatThrownBy(() -> templateService.save(req))
                .isInstanceOf(ServiceException.class).hasMessageContaining("name");

        req.setName("x");
        assertThatThrownBy(() -> templateService.save(req))
                .isInstanceOf(ServiceException.class).hasMessageContaining("schemaJson");

        req.setSchemaJson("[]");
        assertThatThrownBy(() -> templateService.save(req))
                .isInstanceOf(ServiceException.class).hasMessageContaining("formKey");
    }

    @Test
    void createRejectsBadSchemaJson()
    {
        FormTemplateSaveRequest req = new FormTemplateSaveRequest();
        req.setFormKey("k");
        req.setName("x");
        req.setSchemaJson("{not array}");
        when(templateMapper.selectLatestByFormKey(anyString())).thenReturn(null);

        assertThatThrownBy(() -> templateService.save(req))
                .isInstanceOf(ServiceException.class).hasMessageContaining("JSON 数组");
    }

    /* ===== 编辑 ===== */

    @Test
    void editDraftInPlace()
    {
        FormTemplate cur = draft(10L, "k", 1);
        when(templateMapper.selectById(10L)).thenReturn(cur);

        FormTemplateSaveRequest req = new FormTemplateSaveRequest();
        req.setId(10L);
        req.setName("renamed");
        req.setSchemaJson("[]");
        FormTemplate ret = templateService.save(req);

        assertThat(ret.getId()).isEqualTo(10L);
        assertThat(ret.getName()).isEqualTo("renamed");
        verify(templateMapper).updateById(any(FormTemplate.class));
        verify(templateMapper, never()).insert(any(FormTemplate.class));
    }

    @Test
    void editPublishedDerivesNewDraftVersion()
    {
        FormTemplate cur = published(20L, "k", 3);
        when(templateMapper.selectById(20L)).thenReturn(cur);
        when(templateMapper.selectLatestByFormKey("k")).thenReturn(cur);

        FormTemplateSaveRequest req = new FormTemplateSaveRequest();
        req.setId(20L);
        req.setName("v2");
        req.setSchemaJson("[]");

        FormTemplate ret = templateService.save(req);
        assertThat(ret.getStatus()).isEqualTo("DRAFT");
        assertThat(ret.getVersion()).isEqualTo(4);
        verify(templateMapper).insert(any(FormTemplate.class));
    }

    /* ===== 状态机 ===== */

    @Test
    void publishOnlyFromDraft()
    {
        FormTemplate cur = draft(30L, "k", 1);
        when(templateMapper.selectById(30L)).thenReturn(cur);
        when(templateMapper.selectAllByFormKey("k")).thenReturn(List.of(cur));

        FormTemplate ret = templateService.publish(30L);
        assertThat(ret.getStatus()).isEqualTo("PUBLISHED");
        assertThat(ret.getPublishedAt()).isNotNull();
        verify(templateMapper).updateById(cur);
    }

    @Test
    void publishArchivesOtherActiveVersions()
    {
        FormTemplate v1 = published(31L, "k", 1);
        FormTemplate v2 = draft(32L, "k", 2);
        when(templateMapper.selectById(32L)).thenReturn(v2);
        when(templateMapper.selectAllByFormKey("k")).thenReturn(List.of(v1, v2));

        FormTemplate ret = templateService.publish(32L);
        assertThat(ret.getStatus()).isEqualTo("PUBLISHED");
        assertThat(v1.getStatus()).isEqualTo("ARCHIVED");
        verify(templateMapper, times(2)).updateById(any(FormTemplate.class));
    }

    @Test
    void publishRejectsNonDraft()
    {
        FormTemplate p = published(40L, "k", 1);
        when(templateMapper.selectById(40L)).thenReturn(p);
        assertThatThrownBy(() -> templateService.publish(40L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("仅草稿");
    }

    @Test
    void archivePublishedSuccess()
    {
        FormTemplate p = published(50L, "k", 1);
        when(templateMapper.selectById(50L)).thenReturn(p);
        FormTemplate ret = templateService.archive(50L);
        assertThat(ret.getStatus()).isEqualTo("ARCHIVED");
    }

    @Test
    void archiveRejectsDraft()
    {
        FormTemplate d = draft(51L, "k", 1);
        when(templateMapper.selectById(51L)).thenReturn(d);
        assertThatThrownBy(() -> templateService.archive(51L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("仅已发布");
    }

    /* ===== 软删 ===== */

    @Test
    void removeDraftSuccess()
    {
        FormTemplate d = draft(60L, "k", 1);
        when(templateMapper.selectById(60L)).thenReturn(d);
        templateService.remove(60L);
        verify(templateMapper).softDeleteById(eq(60L), eq("designer"));
    }

    @Test
    void removePublishedRejected()
    {
        FormTemplate p = published(61L, "k", 1);
        when(templateMapper.selectById(61L)).thenReturn(p);
        assertThatThrownBy(() -> templateService.remove(61L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("已发布");
        verify(templateMapper, never()).softDeleteById(anyLong(), anyString());
    }

    /* ===== 查询 / activeByKey ===== */

    @Test
    void activeByKeyReturnsPublishedVersion()
    {
        FormTemplate v1 = archived(70L, "k", 1);
        FormTemplate v2 = published(71L, "k", 2);
        FormTemplate v3 = draft(72L, "k", 3);
        when(templateMapper.selectAllByFormKey("k")).thenReturn(List.of(v3, v2, v1));

        FormTemplate ret = templateService.activeByKey("k");
        assertThat(ret).isNotNull();
        assertThat(ret.getId()).isEqualTo(71L);
    }

    @Test
    void activeByKeyReturnsNullWhenNoPublished()
    {
        FormTemplate v1 = draft(80L, "k", 1);
        when(templateMapper.selectAllByFormKey("k")).thenReturn(List.of(v1));
        assertThat(templateService.activeByKey("k")).isNull();
    }

    @Test
    void pageRespectsMaxPageSize()
    {
        when(templateMapper.selectPage(any(), eq(0), eq(200))).thenReturn(List.of());
        when(templateMapper.count(any())).thenReturn(0L);

        FormTemplateQuery q = new FormTemplateQuery();
        q.setPageNum(1);
        q.setPageSize(9999);
        templateService.page(q);

        verify(templateMapper).selectPage(any(), eq(0), eq(200));
    }

    /* ===== helpers ===== */

    private static FormTemplate base(Long id, String key, int version, String status)
    {
        FormTemplate t = new FormTemplate();
        t.setId(id);
        t.setFormKey(key);
        t.setVersion(version);
        t.setStatus(status);
        t.setName("n");
        t.setSchemaJson("[]");
        return t;
    }

    private static FormTemplate draft(Long id, String key, int v) { return base(id, key, v, "DRAFT"); }
    private static FormTemplate published(Long id, String key, int v) { return base(id, key, v, "PUBLISHED"); }
    private static FormTemplate archived(Long id, String key, int v) { return base(id, key, v, "ARCHIVED"); }
}
