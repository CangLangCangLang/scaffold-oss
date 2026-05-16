package com.scaffold.module.report.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import com.scaffold.common.exception.ServiceException;
import com.scaffold.common.utils.SecurityUtils;
import com.scaffold.module.report.domain.SysReportTemplate;
import com.scaffold.module.report.mapper.SysReportTemplateMapper;
import com.scaffold.module.report.runtime.ReportRunner;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TemplateServiceTest
{
    @Mock
    private SysReportTemplateMapper mapper;

    @Mock
    private ReportRunner runner;

    @InjectMocks
    private TemplateService service;

    private MockedStatic<SecurityUtils> sec;

    @BeforeEach
    void setUp()
    {
        sec = mockStatic(SecurityUtils.class);
        sec.when(SecurityUtils::getUsername).thenReturn("tester");
        when(runner.globalRowLimit()).thenReturn(10000);
        when(runner.globalTimeoutMs()).thenReturn(30000);
    }

    @AfterEach
    void tearDown()
    {
        if (sec != null) sec.close();
    }

    @Test
    @DisplayName("新增：SQL 走 SqlGuard，DROP 拒")
    void rejectsBadSql()
    {
        SysReportTemplate t = new SysReportTemplate();
        t.setCode("bad"); t.setName("X"); t.setSqlText("DROP TABLE t");
        assertThrows(ServiceException.class, () -> service.save(t));
    }

    @Test
    @DisplayName("新增：rowLimit 超全局上限拒")
    void rejectsTooLargeRowLimit()
    {
        SysReportTemplate t = new SysReportTemplate();
        t.setCode("x"); t.setName("X"); t.setSqlText("SELECT 1");
        t.setRowLimit(100000);
        assertThrows(ServiceException.class, () -> service.save(t));
    }

    @Test
    @DisplayName("新增：timeoutMs 超全局上限拒")
    void rejectsTooLargeTimeout()
    {
        SysReportTemplate t = new SysReportTemplate();
        t.setCode("x"); t.setName("X"); t.setSqlText("SELECT 1");
        t.setTimeoutMs(60000);
        assertThrows(ServiceException.class, () -> service.save(t));
    }

    @Test
    @DisplayName("新增：rowLimit / timeoutMs 留空 → 默认填全局")
    void defaultsLimits()
    {
        SysReportTemplate t = new SysReportTemplate();
        t.setCode("x"); t.setName("X"); t.setSqlText("SELECT 1");
        when(mapper.selectByCode("x")).thenReturn(null);
        when(mapper.insert(any())).thenAnswer(inv ->
        {
            ((SysReportTemplate) inv.getArgument(0)).setId(7L);
            return 1;
        });

        Long id = service.save(t);
        assertEquals(7L, id);
        assertEquals(10000, t.getRowLimit());
        assertEquals(30000, t.getTimeoutMs());
    }

    @Test
    @DisplayName("新增：code 重复拒")
    void rejectsDuplicateCode()
    {
        SysReportTemplate t = new SysReportTemplate();
        t.setCode("dup"); t.setName("X"); t.setSqlText("SELECT 1");
        when(mapper.selectByCode("dup")).thenReturn(new SysReportTemplate());
        assertThrows(ServiceException.class, () -> service.save(t));
    }

    @Test
    @DisplayName("编辑：code 不允许改动（被静默置空）")
    void editIgnoresCodeChange()
    {
        SysReportTemplate t = new SysReportTemplate();
        t.setId(1L); t.setCode("attempt-change"); t.setSqlText("SELECT 1");
        when(mapper.selectById(1L)).thenReturn(new SysReportTemplate());
        when(mapper.updateById(any())).thenReturn(1);

        service.save(t);
        assertEquals(null, t.getCode());
    }
}
