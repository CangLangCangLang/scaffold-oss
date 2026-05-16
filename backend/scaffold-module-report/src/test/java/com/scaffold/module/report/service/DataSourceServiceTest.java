package com.scaffold.module.report.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import com.scaffold.common.exception.ServiceException;
import com.scaffold.common.utils.SecurityUtils;
import com.scaffold.module.report.domain.SysReportDataSource;
import com.scaffold.module.report.dto.DataSourceUpsertRequest;
import com.scaffold.module.report.mapper.SysReportDataSourceMapper;
import com.scaffold.module.report.runtime.ReportDataSourceManager;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataSourceServiceTest
{
    @Mock
    private SysReportDataSourceMapper mapper;

    @Mock
    private ReportDataSourceManager dataSourceManager;

    @InjectMocks
    private DataSourceService service;

    private MockedStatic<SecurityUtils> securityMock;

    @BeforeEach
    void setUp()
    {
        securityMock = mockStatic(SecurityUtils.class);
        securityMock.when(SecurityUtils::getUsername).thenReturn("tester");
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown()
    {
        if (securityMock != null) securityMock.close();
    }

    @Test
    @DisplayName("列表不返回密文，只回填遮蔽串")
    void listMasksPassword()
    {
        SysReportDataSource a = new SysReportDataSource();
        a.setId(1L); a.setCode("a"); a.setPasswordEnc("ENC(xxx)");
        SysReportDataSource b = new SysReportDataSource();
        b.setId(2L); b.setCode("b"); b.setPasswordEnc(null);
        when(mapper.selectAll()).thenReturn(java.util.Arrays.asList(a, b));

        var list = service.list();
        assertNull(list.get(0).getPasswordEnc());
        assertEquals("********", list.get(0).getPasswordMask());
        assertNull(list.get(1).getPasswordEnc());
        assertEquals("", list.get(1).getPasswordMask());
    }

    @Test
    @DisplayName("新增：code 重复拒")
    void addRejectsDuplicateCode()
    {
        DataSourceUpsertRequest req = new DataSourceUpsertRequest();
        req.setCode("dup"); req.setName("X"); req.setJdbcUrl("jdbc:h2:mem:x");
        when(mapper.selectByCode("dup")).thenReturn(new SysReportDataSource());
        assertThrows(ServiceException.class, () -> service.save(req));
    }

    @Test
    @DisplayName("新增：密码非空 → 走 manager.encrypt 写密文")
    void addEncryptsPassword()
    {
        DataSourceUpsertRequest req = new DataSourceUpsertRequest();
        req.setCode("ok"); req.setName("X"); req.setJdbcUrl("jdbc:h2:mem:x"); req.setPassword("p1");
        when(mapper.selectByCode("ok")).thenReturn(null);
        when(dataSourceManager.encrypt("p1")).thenReturn("ENC(p1)");
        when(mapper.insert(any())).thenAnswer(inv ->
        {
            ((SysReportDataSource) inv.getArgument(0)).setId(7L);
            return 1;
        });

        Long id = service.save(req);
        assertEquals(7L, id);

        ArgumentCaptor<SysReportDataSource> cap = ArgumentCaptor.forClass(SysReportDataSource.class);
        verify(mapper).insert(cap.capture());
        assertEquals("ENC(p1)", cap.getValue().getPasswordEnc());
    }

    @Test
    @DisplayName("编辑：password=null 不动密文")
    void editKeepsPasswordWhenNull()
    {
        DataSourceUpsertRequest req = new DataSourceUpsertRequest();
        req.setId(1L); req.setName("Edit"); req.setJdbcUrl("jdbc:h2");
        // password 留 null
        when(mapper.selectById(1L)).thenReturn(new SysReportDataSource());
        when(mapper.updateById(any())).thenReturn(1);

        Long id = service.save(req);
        assertEquals(1L, id);

        ArgumentCaptor<SysReportDataSource> cap = ArgumentCaptor.forClass(SysReportDataSource.class);
        verify(mapper).updateById(cap.capture());
        assertNull(cap.getValue().getPasswordEnc());
        verify(dataSourceManager, never()).encrypt(anyString());
    }

    @Test
    @DisplayName("编辑：password=空串 → 清空密码（写空串）")
    void editClearsPasswordOnEmpty()
    {
        DataSourceUpsertRequest req = new DataSourceUpsertRequest();
        req.setId(1L); req.setJdbcUrl("jdbc:h2"); req.setPassword("");
        when(mapper.selectById(1L)).thenReturn(new SysReportDataSource());
        when(mapper.updateById(any())).thenReturn(1);

        service.save(req);
        ArgumentCaptor<SysReportDataSource> cap = ArgumentCaptor.forClass(SysReportDataSource.class);
        verify(mapper).updateById(cap.capture());
        assertEquals("", cap.getValue().getPasswordEnc());
    }

    @Test
    @DisplayName("编辑：password=非空 → 重新加密")
    void editReEncrypts()
    {
        DataSourceUpsertRequest req = new DataSourceUpsertRequest();
        req.setId(1L); req.setJdbcUrl("jdbc:h2"); req.setPassword("new-pwd");
        when(mapper.selectById(1L)).thenReturn(new SysReportDataSource());
        when(mapper.updateById(any())).thenReturn(1);
        when(dataSourceManager.encrypt("new-pwd")).thenReturn("ENC(new)");

        service.save(req);
        ArgumentCaptor<SysReportDataSource> cap = ArgumentCaptor.forClass(SysReportDataSource.class);
        verify(mapper).updateById(cap.capture());
        assertEquals("ENC(new)", cap.getValue().getPasswordEnc());
    }

    @Test
    @DisplayName("编辑：保存后清缓存")
    void editInvalidatesCache()
    {
        DataSourceUpsertRequest req = new DataSourceUpsertRequest();
        req.setId(7L); req.setJdbcUrl("jdbc:h2");
        when(mapper.selectById(7L)).thenReturn(new SysReportDataSource());
        when(mapper.updateById(any())).thenReturn(1);

        service.save(req);
        verify(dataSourceManager, times(1)).invalidate(7L);
    }

    @Test
    @DisplayName("删除：清缓存且 mapper 返回 0 → 报错")
    void removeRequiresAffected()
    {
        when(mapper.deleteById(99L)).thenReturn(0);
        assertThrows(ServiceException.class, () -> service.remove(99L));
        verify(dataSourceManager, never()).invalidate(99L);
    }

    @Test
    @DisplayName("删除：成功 → 缓存清掉")
    void removeOk()
    {
        when(mapper.deleteById(11L)).thenReturn(1);
        service.remove(11L);
        verify(dataSourceManager).invalidate(11L);
    }
}
