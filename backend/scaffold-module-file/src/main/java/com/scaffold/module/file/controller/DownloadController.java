package com.scaffold.module.file.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.scaffold.common.annotation.Anonymous;
import com.scaffold.common.annotation.AuditLog;
import com.scaffold.common.config.ScaffoldConfig;
import com.scaffold.common.core.controller.BaseController;
import com.scaffold.common.exception.ServiceException;
import com.scaffold.common.utils.StringUtils;
import com.scaffold.module.file.domain.SysFile;
import com.scaffold.module.file.service.FileService;
import com.scaffold.module.file.service.ShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 文件中心：鉴权下载与分享访问（M-6）。
 *
 * <ul>
 *   <li>{@code GET /file/download/{id}}：登录用户拿 sys_file 主键直接下载（需 file:file:download 权限）</li>
 *   <li>{@code GET /file/share/access/{token}}：分享 token 匿名访问（{@code @Anonymous}）</li>
 * </ul>
 *
 * <p>local 模式：用 {@code Files.newInputStream} 读盘流式输出；S3 / OSS 模式（storage_type != local）：
 * 直接 302 redirect 到 {@code storage_path}（一般是预签名 URL）。
 *
 * @author scaffold
 */
@Tag(name = "文件中心 - 下载（M-6）", description = "鉴权下载 + 分享 token 匿名访问")
@RestController
@RequestMapping("/file")
public class DownloadController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(DownloadController.class);

    @Autowired private FileService fileService;
    @Autowired private ShareService shareService;

    /** 与 framework FileStorageProperties.localRoot 对齐；空时落回 ScaffoldConfig.profile */
    @Value("${file.storage.local-root:}")
    private String localRoot;

    @Operation(summary = "鉴权下载（按 sys_file.id）",
            description = "登录用户走本端点；前端 a 标签直接拼 token 即可。S3 模式时返 302。")
    @PreAuthorize("@ss.hasPermi('file:file:download')")
    @AuditLog(module = "file.file", action = "DOWNLOAD", resourceType = "file",
            resourceId = "#id",
            comment = "'下载文件 ' + #id",
            recordReturn = false)
    @GetMapping("/download/{id}")
    public void download(@PathVariable Long id, HttpServletResponse rsp) throws IOException
    {
        SysFile f = fileService.detail(id);
        if ("2".equals(f.getDelFlag()))
        {
            throw new ServiceException("文件已删除: " + id);
        }
        writeFile(f, rsp);
    }

    @Anonymous
    @Operation(summary = "分享 token 匿名访问（带过期 / 一次性 / 可选密码）",
            description = "无需登录；token 失效 / 过期 / 密码错时 4xx 报错。访问成功 visits +1，"
                    + "一次性会把 status 置为已用尽。S3 模式直接 302。")
    @GetMapping("/share/access/{token}")
    public void share(@PathVariable String token,
                      @Parameter(description = "可选访问密码") @RequestParam(value = "password", required = false) String password,
                      HttpServletResponse rsp) throws IOException
    {
        SysFile f = shareService.access(token, password);
        writeFile(f, rsp);
    }

    private void writeFile(SysFile f, HttpServletResponse rsp) throws IOException
    {
        // S3 / 远程介质：直接 302
        if (!"local".equals(fileService.storageType()))
        {
            rsp.setStatus(HttpStatus.FOUND.value());
            rsp.setHeader("Location", f.getStoragePath());
            return;
        }

        String objectKey = fileService.resolveObjectKey(f);
        if (objectKey == null)
        {
            throw new ServiceException("无法解析对象 key: " + f.getStoragePath());
        }
        String root = StringUtils.isEmpty(localRoot) ? ScaffoldConfig.getProfile() : localRoot;
        Path target = Paths.get(root, objectKey).toAbsolutePath().normalize();
        Path rootPath = Paths.get(root).toAbsolutePath().normalize();
        if (!target.startsWith(rootPath))
        {
            log.warn("path traversal blocked: requested={}, root={}", target, rootPath);
            throw new ServiceException("非法路径");
        }
        if (!Files.exists(target))
        {
            throw new ServiceException("物理文件已不存在: " + objectKey);
        }

        rsp.setContentType(f.getMime() == null ? "application/octet-stream" : f.getMime());
        rsp.setContentLengthLong(Files.size(target));
        String fname = f.getName() == null ? f.getOriginalName() : f.getName();
        if (fname == null) fname = objectKey;
        String encoded = URLEncoder.encode(fname, StandardCharsets.UTF_8).replace("+", "%20");
        rsp.setHeader("Content-Disposition", "attachment; filename=\"" + fname + "\"; filename*=UTF-8''" + encoded);
        Files.copy(target, rsp.getOutputStream());
        rsp.getOutputStream().flush();
    }
}
