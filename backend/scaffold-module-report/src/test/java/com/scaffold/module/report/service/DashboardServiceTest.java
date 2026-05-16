package com.scaffold.module.report.service;

import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import com.scaffold.common.exception.ServiceException;
import com.scaffold.common.utils.SecurityUtils;
import com.scaffold.module.report.domain.SysReportDashboard;
import com.scaffold.module.report.domain.SysReportDashboardCard;
import com.scaffold.module.report.mapper.SysReportDashboardCardMapper;
import com.scaffold.module.report.mapper.SysReportDashboardMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest
{
    @Mock
    private SysReportDashboardMapper dashMapper;

    @Mock
    private SysReportDashboardCardMapper cardMapper;

    @InjectMocks
    private DashboardService service;

    private MockedStatic<SecurityUtils> sec;

    @BeforeEach
    void setUp()
    {
        sec = mockStatic(SecurityUtils.class);
        sec.when(SecurityUtils::getUsername).thenReturn("tester");
    }

    @AfterEach
    void tearDown()
    {
        if (sec != null) sec.close();
    }

    @Test
    @DisplayName("详情：dashboard + cards 一并返回")
    void detailReturnsBoth()
    {
        SysReportDashboard d = new SysReportDashboard();
        d.setId(1L);
        when(dashMapper.selectById(1L)).thenReturn(d);
        when(cardMapper.selectByDashboardId(1L)).thenReturn(java.util.Collections.emptyList());

        Map<String, Object> r = service.detail(1L);
        assertNotNull(r.get("dashboard"));
        assertNotNull(r.get("cards"));
    }

    @Test
    @DisplayName("详情：不存在抛业务异常")
    void detailNotFound()
    {
        when(dashMapper.selectById(99L)).thenReturn(null);
        assertThrows(ServiceException.class, () -> service.detail(99L));
    }

    @Test
    @DisplayName("新增：code 重复拒")
    void addRejectsDuplicateCode()
    {
        SysReportDashboard d = new SysReportDashboard();
        d.setCode("dup");
        when(dashMapper.selectByCode("dup")).thenReturn(new SysReportDashboard());
        assertThrows(ServiceException.class, () -> service.save(d, null));
    }

    @Test
    @DisplayName("新增：cards 整批插入，dashboardId 自动回填")
    void addInsertsCards()
    {
        SysReportDashboard d = new SysReportDashboard();
        d.setCode("ok");
        when(dashMapper.selectByCode("ok")).thenReturn(null);
        when(dashMapper.insert(any())).thenAnswer(inv ->
        {
            ((SysReportDashboard) inv.getArgument(0)).setId(99L);
            return 1;
        });

        SysReportDashboardCard c1 = new SysReportDashboardCard();
        c1.setTitle("kpi"); c1.setTemplateId(1L);
        SysReportDashboardCard c2 = new SysReportDashboardCard();
        c2.setTitle("trend"); c2.setTemplateId(2L);

        Long id = service.save(d, Arrays.asList(c1, c2));
        assertEquals(99L, id);
        verify(cardMapper).deleteByDashboardId(99L);
        verify(cardMapper, times(2)).insert(any(SysReportDashboardCard.class));
    }

    @Test
    @DisplayName("编辑：禁止 code 改动")
    void editKeepsCode()
    {
        SysReportDashboard d = new SysReportDashboard();
        d.setId(1L);
        d.setCode("attempt-change");
        when(dashMapper.selectById(1L)).thenReturn(new SysReportDashboard());

        service.save(d, null);
        ArgumentCaptor<SysReportDashboard> cap = ArgumentCaptor.forClass(SysReportDashboard.class);
        verify(dashMapper).updateById(cap.capture());
        assertEquals(null, cap.getValue().getCode());
    }

    @Test
    @DisplayName("删除：先清卡片再删板")
    void removeCascades()
    {
        when(dashMapper.deleteById(7L)).thenReturn(1);
        service.remove(7L);
        verify(cardMapper).deleteByDashboardId(7L);
        verify(dashMapper).deleteById(7L);
    }

    @Test
    @DisplayName("删除：板不存在 → 抛错；卡片删除已发生（先删 → 再发现板没了）")
    void removeMissingDashboard()
    {
        when(dashMapper.deleteById(7L)).thenReturn(0);
        assertThrows(ServiceException.class, () -> service.remove(7L));
        verify(cardMapper).deleteByDashboardId(7L);
    }
}
