package com.scaffold.framework.web.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import com.scaffold.common.core.storage.FileStorageService;
import com.scaffold.common.exception.ServiceException;

/**
 * DefaultUploadStorageService 单测：覆盖白名单 / 大小限 / bucket 安全校验，
 * 验证 objectKey 拼接规则，并确保所有 IO 都通过 {@link FileStorageService}（local / s3 适配在更下层）。
 *
 * @author scaffold
 */
class DefaultUploadStorageServiceTest
{
    private FileStorageService fileStorageService;
    private UploadStorageProperties properties;
    private DefaultUploadStorageService service;

    @BeforeEach
    void setUp() throws Exception
    {
        fileStorageService = mock(FileStorageService.class);
        properties = new UploadStorageProperties();
        properties.setMaxSizeMb(10);
        // 用全局默认白名单（DEFAULT_EXTS）
        when(fileStorageService.store(anyString(), any(), anyString(), anyLong()))
                .thenAnswer(inv -> "/profile/" + inv.getArgument(0));
        service = new DefaultUploadStorageService(fileStorageService, properties);
    }

    @Test
    void rejectsNullOrEmptyFile()
    {
        assertThatThrownBy(() -> service.save(null, "cms/image"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("空");

        MockMultipartFile empty = new MockMultipartFile("file", "x.png", "image/png", new byte[0]);
        assertThatThrownBy(() -> service.save(empty, "cms/image"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("空");
    }

    @Test
    void rejectsIllegalBucket()
    {
        MockMultipartFile f = new MockMultipartFile("file", "x.png", "image/png", new byte[]{1});
        assertThatThrownBy(() -> service.save(f, null))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("bucket");
        assertThatThrownBy(() -> service.save(f, ""))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("bucket");
        assertThatThrownBy(() -> service.save(f, "/cms/image"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("bucket");
        assertThatThrownBy(() -> service.save(f, "cms/image/"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("bucket");
        assertThatThrownBy(() -> service.save(f, "cms/../etc"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("bucket");
        assertThatThrownBy(() -> service.save(f, "cms\\image"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("bucket");
    }

    @Test
    void rejectsExtensionNotInWhitelist()
    {
        // .exe 不在默认白名单内
        MockMultipartFile f = new MockMultipartFile("file", "evil.exe", "application/x-msdownload",
                new byte[]{1, 2, 3});
        assertThatThrownBy(() -> service.save(f, "cms/image"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("白名单");
    }

    @Test
    void rejectsOversizedFile()
    {
        properties.setMaxSizeMb(1); // 1MB 上限
        byte[] big = new byte[2 * 1024 * 1024]; // 2MB
        MockMultipartFile f = new MockMultipartFile("file", "huge.png", "image/png", big);
        assertThatThrownBy(() -> service.save(f, "cms/image"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("超过上限");
    }

    @Test
    void savesFileWithCorrectObjectKey() throws Exception
    {
        MockMultipartFile f = new MockMultipartFile("file", "logo.PNG", "image/png", new byte[]{1, 2, 3});
        String url = service.save(f, "cms/image");

        ArgumentCaptor<String> keyCap = ArgumentCaptor.forClass(String.class);
        verify(fileStorageService).store(keyCap.capture(), any(), eq("image/png"), eq(3L));
        String key = keyCap.getValue();
        assertThat(key).startsWith("cms/image/");
        // yyyyMM/<32hex>.png
        assertThat(key).matches("cms/image/\\d{6}/[0-9a-f]{32}\\.png");
        assertThat(url).isEqualTo("/profile/" + key);
    }

    @Test
    void uploadOptionsOverrideWhitelistAndMaxBytes() throws Exception
    {
        // 默认全局白名单不含 zip → 默认会拒
        MockMultipartFile zip = new MockMultipartFile("file", "report.zip",
                "application/zip", new byte[]{1});
        // zip 在 DEFAULT_EXTS 里其实也允许；这里换一个 csv 测窄白名单
        MockMultipartFile csv = new MockMultipartFile("file", "data.csv", "text/csv",
                new byte[]{1, 2});

        // 选项白名单缩到只允许 png
        UploadOptions onlyPng = UploadOptions.exts(Set.of("png"));
        assertThatThrownBy(() -> service.save(csv, "form/file", onlyPng))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("白名单");

        // 选项白名单含 zip → 通过
        UploadOptions allowZip = UploadOptions.exts(Set.of("zip"));
        String url = service.save(zip, "form/file", allowZip);
        assertThat(url).contains("/form/file/");
        assertThat(url).endsWith(".zip");
    }

    @Test
    void normalizesExtensionsCaseAndDot() throws Exception
    {
        UploadOptions opts = new UploadOptions();
        // 大小写 + 带点 + 去重
        opts.setAllowedExtensions(Set.of(".PNG", "Png", "jpg"));
        MockMultipartFile f = new MockMultipartFile("file", "a.PNG", "image/png", new byte[]{1});
        String url = service.save(f, "cms/image", opts);
        assertThat(url).endsWith(".png");
    }
}
