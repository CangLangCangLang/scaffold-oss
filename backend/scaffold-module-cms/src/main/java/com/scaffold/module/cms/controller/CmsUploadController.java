package com.scaffold.module.cms.controller;

import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.scaffold.common.annotation.AuditLog;
import com.scaffold.common.core.controller.BaseController;
import com.scaffold.common.core.domain.AjaxResult;
import com.scaffold.framework.web.storage.UploadOptions;
import com.scaffold.framework.web.storage.UploadStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * CMS 富文本编辑器图片上传端点。<br>
 * 鉴权：需要 {@code cms:upload:image}（即至少能编辑文章的人才允许上传）。<br>
 * 返回：{@code { url: "/profile/cms/image/202605/abcd1234.jpg" }}，
 * 与 wangEditor 的 {@code customInsert} 钩子兼容；前端会拼上 baseURL 直插到 HTML。
 *
 * <p>实现切换为 framework 层 {@link UploadStorageService}：bucket 用 "cms/image" 命名空间，
 * 对外 URL 形态保持不变（依旧是 {@code /profile/cms/image/yyyyMM/<uuid>.<ext>}），
 * 老前端代码 / 老审计记录的 URL 都不出现破坏性变化。
 */
@Tag(name = "CMS 富文本图片上传", description = "wangEditor 富文本图片落本地磁盘 / 对象存储；返回 URL 由前端拼回 HTML")
@RestController
@RequestMapping("/cms/upload")
public class CmsUploadController extends BaseController
{
    /** CMS 图片专用白名单（默认仅图片格式，禁 pdf/zip 等通用类型）*/
    private static final Set<String> CMS_IMAGE_EXTS =
            Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg");

    @Autowired private UploadStorageService uploadStorageService;

    /**
     * CMS 上传专用大小上限（MB），保留原 {@code app.module.cms.upload.max-size-mb} 配置以向后兼容；
     * 同时落到全局 {@code app.upload.max-size-mb} 也行——不传则用 framework 默认。
     */
    @Value("${app.module.cms.upload.max-size-mb:0}")
    private int legacyMaxSizeMb;

    @Operation(summary = "上传富文本图片",
            description = "multipart/form-data 上传；扩展名白名单 jpg/jpeg/png/gif/webp/bmp/svg；大小上限默认 10MB（app.module.cms.upload.max-size-mb 可改）")
    @PreAuthorize("@ss.hasPermi('cms:upload:image')")
    @AuditLog(module = "cms.upload", action = "UPLOAD_IMAGE", resourceType = "image",
            resourceId = "#result?.data?.url",
            comment = "'CMS 上传图片 ' + (#file != null ? #file.originalFilename : '?')",
            recordReturn = false)
    @PostMapping("/image")
    public AjaxResult uploadImage(@RequestParam("file") MultipartFile file)
    {
        UploadOptions opts = new UploadOptions();
        opts.setAllowedExtensions(CMS_IMAGE_EXTS);
        if (legacyMaxSizeMb > 0)
        {
            opts.setMaxBytes((long) legacyMaxSizeMb * 1024L * 1024L);
        }
        String url = uploadStorageService.save(file, "cms/image", opts);
        return success(java.util.Collections.singletonMap("url", url));
    }
}
