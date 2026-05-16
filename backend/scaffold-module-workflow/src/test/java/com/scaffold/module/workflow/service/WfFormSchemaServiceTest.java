package com.scaffold.module.workflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.scaffold.common.exception.ServiceException;
import com.scaffold.module.workflow.domain.WfFormSchema;
import com.scaffold.module.workflow.mapper.WfFormSchemaMapper;

/**
 * WfFormSchemaService 单测：版本递增 + 旧版本停用 + 字段必填校验。
 */
class WfFormSchemaServiceTest
{
    private WfFormSchemaMapper mapper;
    private WfFormSchemaService service;

    @BeforeEach
    void setUp()
    {
        mapper = mock(WfFormSchemaMapper.class);
        service = new WfFormSchemaService();
        org.springframework.test.util.ReflectionTestUtils.setField(service, "formSchemaMapper", mapper);
    }

    @Test
    void rejectsBlankProcessKey()
    {
        WfFormSchema s = new WfFormSchema();
        s.setSchemaJson("[]");
        assertThatThrownBy(() -> service.saveAsNewVersion(s, "u1"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("processDefinitionKey");
    }

    @Test
    void rejectsBlankSchemaJson()
    {
        WfFormSchema s = new WfFormSchema();
        s.setProcessDefinitionKey("leave");
        assertThatThrownBy(() -> service.saveAsNewVersion(s, "u1"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("schemaJson");
    }

    @Test
    void firstSaveStartsAtVersion1AndDisablesNothing()
    {
        when(mapper.selectActiveLatest("leave", WfFormSchema.ACTIVITY_START_FORM)).thenReturn(null);

        WfFormSchema s = new WfFormSchema();
        s.setProcessDefinitionKey("leave");
        s.setSchemaJson("[]");

        WfFormSchema saved = service.saveAsNewVersion(s, "alice");

        assertThat(saved.getVersion()).isEqualTo(1);
        assertThat(saved.getEnabled()).isTrue();
        assertThat(saved.getActivityId()).isEqualTo(WfFormSchema.ACTIVITY_START_FORM);
        verify(mapper).insert(s);
        verify(mapper, times(1)).disableOldVersions(eq("leave"),
                eq(WfFormSchema.ACTIVITY_START_FORM), eq(s.getId()));
    }

    @Test
    void subsequentSaveIncrementsVersionAndDisablesPrevious()
    {
        WfFormSchema prev = new WfFormSchema();
        prev.setVersion(3);
        prev.setEnabled(true);
        when(mapper.selectActiveLatest("leave", WfFormSchema.ACTIVITY_START_FORM)).thenReturn(prev);

        WfFormSchema s = new WfFormSchema();
        s.setProcessDefinitionKey("leave");
        s.setSchemaJson("[{\"type\":\"input\",\"field\":\"days\"}]");

        WfFormSchema saved = service.saveAsNewVersion(s, "bob");

        assertThat(saved.getVersion()).isEqualTo(4);
        verify(mapper).insert(s);
        verify(mapper).disableOldVersions("leave", WfFormSchema.ACTIVITY_START_FORM, s.getId());
    }

    @Test
    void deleteWhenNotFoundReturnsZero()
    {
        when(mapper.selectById(99L)).thenReturn(null);
        int n = service.delete(99L, "alice");
        assertThat(n).isEqualTo(0);
        verify(mapper, never()).deleteById(99L);
    }

    @Test
    void deleteRemovesWhenFound()
    {
        WfFormSchema row = new WfFormSchema();
        row.setId(7L);
        when(mapper.selectById(7L)).thenReturn(row);
        when(mapper.deleteById(7L)).thenReturn(1);

        int n = service.delete(7L, "alice");
        assertThat(n).isEqualTo(1);
        verify(mapper).deleteById(7L);
    }
}
