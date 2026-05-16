package com.scaffold.framework.web.storage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
import com.scaffold.common.core.storage.FileStorageService;
import com.scaffold.common.exception.ServiceException;

/**
 * 默认上传 wrapper 实现：负责"上传场景"安全控制（扩展名白名单 / 大小限），
 * 落盘委托给 {@link FileStorageService}（local / s3 自动适配）。
 *
 * <h3>对象 key 规则</h3>
 * <pre>{@code <bucket>/yyyyMM/<uuid>.<ext>}</pre>
 * 例如 bucket="cms/image"、上传 logo.PNG，得到 {@code cms/image/202605/abcd1234.png}。
 *
 * <h3>默认白名单</h3>
 * 图片 + 常见办公文档：jpg / jpeg / png / gif / webp / bmp / svg / pdf / doc / docx / xls / xlsx / ppt / pptx / txt / csv / zip
 *
 * @author scaffold
 */
public class DefaultUploadStorageService implements UploadStorageService
{
    private static final Logger log = LoggerFactory.getLogger(DefaultUploadStorageService.class);

    private static final Set<String> DEFAULT_EXTS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt", "csv", "zip");

    private final FileStorageService fileStorageService;
    private final UploadStorageProperties properties;

    public DefaultUploadStorageService(FileStorageService fileStorageService, UploadStorageProperties properties)
    {
        this.fileStorageService = fileStorageService;
        this.properties = properties;
    }

    @Override
    public String save(MultipartFile file, String bucket)
    {
        return save(file, bucket, null);
    }

    @Override
    public String save(MultipartFile file, String bucket, UploadOptions options)
    {
        if (file == null || file.isEmpty())
        {
            throw new ServiceException("上传文件为空");
        }
        validateBucket(bucket);

        long maxBytes = resolveMaxBytes(options);
        if (file.getSize() > maxBytes)
        {
            throw new ServiceException("文件超过上限 " + (maxBytes / 1024L / 1024L) + "MB");
        }

        String ext = extOf(file.getOriginalFilename());
        Set<String> allowed = resolveAllowedExtensions(options);
        if (!allowed.contains(ext))
        {
            throw new ServiceException("文件扩展名不在白名单内: " + ext + "，允许: " + allowed);
        }

        String yyyyMM = new SimpleDateFormat("yyyyMM").format(new Date());
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        String objectKey = bucket + "/" + yyyyMM + "/" + filename;

        try (InputStream in = file.getInputStream())
        {
            String url = fileStorageService.store(objectKey, in, file.getContentType(), file.getSize());
            log.info("upload OK bucket={} key={} size={}KB type={}",
                    bucket, objectKey, file.getSize() / 1024, fileStorageService.type());
            return url;
        }
        catch (IOException e)
        {
            log.warn("upload FAIL bucket={} key={} reason={}", bucket, objectKey, e.getMessage());
            throw new ServiceException("文件保存失败: " + e.getMessage());
        }
    }

    /**
     * 校验 bucket：必填、不可含 ".."、不可以 "/" 起始或结尾、不可含反斜杠。
     * 允许 "cms/image"、"form/file" 这种带命名空间的多级路径，给 framework 层多业务复用。
     */
    static void validateBucket(String bucket)
    {
        if (bucket == null || bucket.isBlank())
        {
            throw new ServiceException("bucket 不能为空");
        }
        if (bucket.contains("..") || bucket.contains("\\") || bucket.startsWith("/") || bucket.endsWith("/"))
        {
            throw new ServiceException("非法 bucket: " + bucket);
        }
    }

    private long resolveMaxBytes(UploadOptions options)
    {
        if (options != null && options.getMaxBytes() != null && options.getMaxBytes() > 0)
        {
            return options.getMaxBytes();
        }
        int mb = properties.getMaxSizeMb() > 0 ? properties.getMaxSizeMb() : 10;
        return (long) mb * 1024L * 1024L;
    }

    private Set<String> resolveAllowedExtensions(UploadOptions options)
    {
        if (options != null && options.getAllowedExtensions() != null && !options.getAllowedExtensions().isEmpty())
        {
            return normalize(options.getAllowedExtensions());
        }
        if (properties.getAllowedExtensions() != null && !properties.getAllowedExtensions().isEmpty())
        {
            return normalize(properties.getAllowedExtensions());
        }
        return DEFAULT_EXTS;
    }

    private static Set<String> normalize(Set<String> src)
    {
        Set<String> out = new java.util.HashSet<>(src.size());
        for (String s : src)
        {
            if (s == null) continue;
            String t = s.trim().toLowerCase(Locale.ROOT);
            if (t.startsWith(".")) t = t.substring(1);
            if (!t.isEmpty()) out.add(t);
        }
        return out.isEmpty() ? DEFAULT_EXTS : out;
    }

    private static String extOf(String name)
    {
        if (name == null) return "";
        String safe = new File(name).getName();
        int idx = safe.lastIndexOf('.');
        if (idx < 0 || idx == safe.length() - 1) return "";
        return safe.substring(idx + 1).toLowerCase(Locale.ROOT);
    }
}
