package com.scaffold.web.controller.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.scaffold.common.annotation.AuditLog;
import com.scaffold.common.core.controller.BaseController;
import com.scaffold.common.core.domain.AjaxResult;
import com.scaffold.framework.web.storage.UploadStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 通用文件上传端点：所有业务模块都可以走这个 endpoint，落盘到 {@code /profile/<bucket>/...}。
 *
 * <p>与 {@code /common/upload} 的差别：
 * <ul>
 *   <li>带鉴权：{@code system:upload:file}（默认 admin 角色拥有）</li>
 *   <li>带 @AuditLog：每次上传写入 sys_audit_log</li>
 *   <li>走 {@link UploadStorageService}：扩展名白名单 + 大小限 + bucket 命名空间隔离</li>
 *   <li>底层 {@code FileStorageService} 适配，切 S3 / OSS 业务侧无感</li>
 * </ul>
 *
 * <p>典型调用：
 * <pre>
 *   POST /system/upload/file
 *   Content-Type: multipart/form-data
 *   file: &lt;binary&gt;
 *   bucket: form/file        (可选，默认 "common")
 * </pre>
 *
 * @author scaffold
 */
@Tag(name = "通用文件上传", description = "需要 system:upload:file 权限；按 bucket 落到 /profile/<bucket>/yyyyMM/<uuid>.<ext>")
@RestController
@RequestMapping("/system/upload")
public class SystemUploadController extends BaseController
{
    private static final String DEFAULT_BUCKET = "common";

    @Autowired private UploadStorageService uploadStorageService;

    @Operation(summary = "上传单个文件",
            description = "multipart/form-data；返回 url 字段直插业务表单。bucket 必须由调用方传入并保持稳定（如 'form/file'）")
    @PreAuthorize("@ss.hasPermi('system:upload:file')")
    @AuditLog(module = "system.upload", action = "UPLOAD_FILE", resourceType = "file",
            resourceId = "#result?.data?.url",
            comment = "'通用上传 ' + (#file != null ? #file.originalFilename : '?') + ' bucket=' + (#bucket ?: 'common')",
            recordReturn = false)
    @PostMapping("/file")
    public AjaxResult uploadFile(
            @Parameter(description = "上传内容") @RequestParam("file") MultipartFile file,
            @Parameter(description = "业务 bucket（如 form/file），默认 common") @RequestParam(value = "bucket", required = false) String bucket)
    {
        String b = (bucket == null || bucket.isBlank()) ? DEFAULT_BUCKET : bucket.trim();
        String url = uploadStorageService.save(file, b);
        return success(java.util.Map.of(
                "url", url,
                "originalFilename", file.getOriginalFilename(),
                "size", file.getSize(),
                "bucket", b));
    }
}
