package com.scaffold.module.file.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.scaffold.common.annotation.AuditLog;
import com.scaffold.common.core.controller.BaseController;
import com.scaffold.common.core.domain.AjaxResult;
import com.scaffold.module.file.dto.ShareCreateRequest;
import com.scaffold.module.file.service.ShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 文件中心：分享链接（M-6）。
 *
 * <p>访问入口在 {@link DownloadController#share(String, String)}（不需要登录），本 controller 仅管 CRUD。
 *
 * @author scaffold
 */
@Tag(name = "文件中心 - 分享（M-6）", description = "分享链接 CRUD：过期 / 一次性 / 可选密码")
@RestController
@RequestMapping("/file/share")
public class ShareController extends BaseController
{
    @Autowired private ShareService shareService;

    @Operation(summary = "查询当前用户创建的分享列表")
    @PreAuthorize("@ss.hasPermi('file:share:list')")
    @GetMapping
    public AjaxResult listMine()
    {
        return success(shareService.listMine());
    }

    @Operation(summary = "创建分享")
    @PreAuthorize("@ss.hasPermi('file:share:add')")
    @AuditLog(module = "file.share", action = "CREATE", resourceType = "share",
            resourceId = "#result?.data?.id",
            comment = "'创建分享 fileId=' + #req.fileId")
    @PostMapping
    public AjaxResult add(@RequestBody ShareCreateRequest req)
    {
        return success(shareService.create(req));
    }

    @Operation(summary = "停用分享（status=1）")
    @PreAuthorize("@ss.hasPermi('file:share:disable')")
    @AuditLog(module = "file.share", action = "DISABLE", resourceType = "share",
            resourceId = "#id",
            comment = "'停用分享 ' + #id")
    @PutMapping("/{id}/disable")
    public AjaxResult disable(@PathVariable Long id)
    {
        return success(shareService.disable(id));
    }

    @Operation(summary = "删除分享")
    @PreAuthorize("@ss.hasPermi('file:share:remove')")
    @AuditLog(module = "file.share", action = "REMOVE", resourceType = "share",
            resourceId = "#id",
            comment = "'删分享 ' + #id",
            recordReturn = false)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id)
    {
        return success(shareService.remove(id));
    }
}
