package com.scaffold.module.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import com.scaffold.common.core.domain.entity.SysUser;
import com.scaffold.common.core.domain.model.LoginUser;
import com.scaffold.common.core.storage.FileStorageService;
import com.scaffold.common.exception.ServiceException;
import com.scaffold.framework.web.storage.UploadStorageService;
import com.scaffold.module.file.domain.SysFile;
import com.scaffold.module.file.dto.FileEditRequest;
import com.scaffold.module.file.mapper.SysFileMapper;
import com.scaffold.module.file.mapper.SysFileRefMapper;

/**
 * FileService 单测。
 *
 * <p>覆盖：上传 happy-path、软删 / 引用计数保护、resolveObjectKey 反推、purgeExpired 流程。
 */
class FileServiceTest
{
    private SysFileMapper fileMapper;
    private SysFileRefMapper refMapper;
    private UploadStorageService uploadStorageService;
    private FileStorageService fileStorageService;
    private FileService fileService;

    @BeforeEach
    void setUp()
    {
        fileMapper = mock(SysFileMapper.class);
        refMapper = mock(SysFileRefMapper.class);
        uploadStorageService = mock(UploadStorageService.class);
        fileStorageService = mock(FileStorageService.class);

        fileService = new FileService();
        ReflectionTestUtils.setField(fileService, "sysFileMapper", fileMapper);
        ReflectionTestUtils.setField(fileService, "sysFileRefMapper", refMapper);
        ReflectionTestUtils.setField(fileService, "uploadStorageService", uploadStorageService);
        ReflectionTestUtils.setField(fileService, "fileStorageService", fileStorageService);
        ReflectionTestUtils.setField(fileService, "urlPrefix", "/profile");

        SysUser sys = new SysUser();
        sys.setUserId(7L);
        sys.setUserName("alice");
        sys.setNickName("Alice");
        LoginUser u = new LoginUser(7L, 1L, sys, null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(u, null, Collections.emptyList()));
    }

    @AfterEach
    void tearDown()
    {
        SecurityContextHolder.clearContext();
    }

    @Test
    void uploadDefaultsToCommonBucketAndPersistsRow()
    {
        when(uploadStorageService.save(any(), anyString()))
                .thenReturn("/profile/common/202605/abc.png");
        MockMultipartFile mf = new MockMultipartFile("file", "logo.PNG", "image/png", new byte[]{1, 2, 3});

        SysFile saved = fileService.upload(mf, null, null);

        assertThat(saved.getBucket()).isEqualTo("common");
        assertThat(saved.getStoragePath()).isEqualTo("/profile/common/202605/abc.png");
        assertThat(saved.getOriginalName()).isEqualTo("logo.PNG");
        assertThat(saved.getExt()).isEqualTo("png");
        assertThat(saved.getRefCount()).isZero();
        assertThat(saved.getDelFlag()).isEqualTo("0");
        assertThat(saved.getCreateBy()).isEqualTo("alice");
        verify(fileMapper).insert(saved);
    }

    @Test
    void uploadHonorsCustomBucketAndFolder()
    {
        when(uploadStorageService.save(any(), anyString()))
                .thenReturn("/profile/cms/image/202605/xyz.jpg");
        MockMultipartFile mf = new MockMultipartFile("file", "x.JPG", "image/jpeg", new byte[]{1});
        SysFile saved = fileService.upload(mf, "cms/image", 42L);

        assertThat(saved.getBucket()).isEqualTo("cms/image");
        assertThat(saved.getFolderId()).isEqualTo(42L);
        verify(uploadStorageService).save(mf, "cms/image");
    }

    @Test
    void detailRejectsMissingId()
    {
        when(fileMapper.selectById(anyLong())).thenReturn(null);
        assertThatThrownBy(() -> fileService.detail(99L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    void editRejectsRowNotFoundOrAlreadyDeleted()
    {
        FileEditRequest req = new FileEditRequest();
        req.setId(10L);
        req.setName("renamed");

        when(fileMapper.selectById(10L)).thenReturn(null);
        assertThatThrownBy(() -> fileService.edit(req))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("不存在");

        SysFile deleted = new SysFile();
        deleted.setId(10L);
        deleted.setDelFlag("2");
        when(fileMapper.selectById(10L)).thenReturn(deleted);
        assertThatThrownBy(() -> fileService.edit(req))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("已删除");
    }

    @Test
    void editPatchesOnlyAllowedFields()
    {
        SysFile cur = new SysFile();
        cur.setId(10L);
        cur.setDelFlag("0");
        cur.setRefCount(0);
        when(fileMapper.selectById(10L)).thenReturn(cur);
        when(fileMapper.updateById(any())).thenReturn(1);

        FileEditRequest req = new FileEditRequest();
        req.setId(10L);
        req.setName("renamed.pdf");
        req.setFolderId(99L);
        req.setCategory("HR");
        req.setTags("tag1,tag2");
        req.setRemark("desc");

        int rows = fileService.edit(req);
        assertThat(rows).isEqualTo(1);
        verify(fileMapper).updateById(any(SysFile.class));
    }

    @Test
    void softRemoveBlockedByPositiveRefCount()
    {
        SysFile cur = new SysFile();
        cur.setId(10L);
        cur.setDelFlag("0");
        cur.setRefCount(2);
        when(fileMapper.selectById(10L)).thenReturn(cur);
        assertThatThrownBy(() -> fileService.softRemove(10L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("引用");
    }

    @Test
    void softRemoveSucceedsWhenRefCountIsZero()
    {
        SysFile cur = new SysFile();
        cur.setId(10L);
        cur.setDelFlag("0");
        cur.setRefCount(0);
        when(fileMapper.selectById(10L)).thenReturn(cur);
        when(fileMapper.softDeleteById(anyLong(), anyString())).thenReturn(1);

        assertThat(fileService.softRemove(10L)).isEqualTo(1);
        verify(fileMapper).softDeleteById(10L, "alice");
    }

    @Test
    void hardRemoveDeletesPhysicalAndRefsAndRow()
    {
        SysFile cur = new SysFile();
        cur.setId(10L);
        cur.setDelFlag("2");
        cur.setRefCount(0);
        cur.setStoragePath("/profile/common/202605/x.png");
        when(fileMapper.selectById(10L)).thenReturn(cur);
        when(fileStorageService.type()).thenReturn("local");
        when(fileStorageService.delete(anyString())).thenReturn(true);
        when(fileMapper.hardDeleteById(10L)).thenReturn(1);

        assertThat(fileService.hardRemove(10L)).isEqualTo(1);
        verify(fileStorageService).delete("common/202605/x.png");
        verify(refMapper).deleteByFileId(10L);
        verify(fileMapper).hardDeleteById(10L);
    }

    @Test
    void hardRemoveBlockedByPositiveRefCount()
    {
        SysFile cur = new SysFile();
        cur.setId(10L);
        cur.setDelFlag("2");
        cur.setRefCount(1);
        when(fileMapper.selectById(10L)).thenReturn(cur);
        assertThatThrownBy(() -> fileService.hardRemove(10L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("引用");
    }

    @Test
    void resolveObjectKeyHandlesPrefixVariants()
    {
        SysFile a = new SysFile();
        a.setStoragePath("/profile/cms/image/202605/abc.png");
        assertThat(fileService.resolveObjectKey(a)).isEqualTo("cms/image/202605/abc.png");

        SysFile b = new SysFile();
        b.setStoragePath("https://cdn.example.com/abc.png");
        assertThat(fileService.resolveObjectKey(b)).isNull();
    }

    @Test
    void purgeExpiredIteratesAndCallsHardDeletePerRow()
    {
        SysFile a = new SysFile(); a.setId(1L); a.setStoragePath("/profile/x/y/a.png"); a.setRefCount(0);
        SysFile b = new SysFile(); b.setId(2L); b.setStoragePath("/profile/x/y/b.png"); b.setRefCount(0);
        when(fileMapper.selectExpiredSoftDeleted(any())).thenReturn(List.of(a, b));
        when(fileStorageService.type()).thenReturn("local");
        when(fileStorageService.delete(anyString())).thenReturn(true);
        when(fileMapper.hardDeleteById(anyLong())).thenReturn(1);

        int n = fileService.purgeExpired(30);
        assertThat(n).isEqualTo(2);
        verify(fileMapper, times(2)).hardDeleteById(anyLong());
        verify(refMapper, times(2)).deleteByFileId(anyLong());
    }
}
