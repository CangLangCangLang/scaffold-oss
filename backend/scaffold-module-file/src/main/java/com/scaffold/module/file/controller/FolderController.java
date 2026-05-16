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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.scaffold.common.annotation.AuditLog;
import com.scaffold.common.core.controller.BaseController;
import com.scaffold.common.core.domain.AjaxResult;
import com.scaffold.common.utils.SecurityUtils;
import com.scaffold.module.file.dto.FolderRequest;
import com.scaffold.module.file.service.FolderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 文件中心：文件夹（M-6）。
 * 普通用户只看自己；admin（{@code file:list}）可指定 owner 看他人。
 *
 * @author scaffold
 */
@Tag(name = "文件中心 - 文件夹（M-6）", description = "用户级隔离的文件夹树（path 拼接 + 唯一约束）")
@RestController
@RequestMapping("/file/folder")
public class FolderController extends BaseController
{
    @Autowired private FolderService folderService;

    @Operation(summary = "查询当前用户的文件夹（admin 可传 owner 看他人）")
    @PreAuthorize("@ss.hasAnyPermi('file:list,file:list:mine')")
    @GetMapping
    public AjaxResult list(@Parameter(description = "owner username（仅 admin 时生效）") @RequestParam(value = "owner", required = false) String owner)
    {
        boolean canSeeAll = SecurityUtils.hasPermi("file:list");
        if (canSeeAll && owner != null && !owner.isBlank())
        {
            return success(folderService.listByOwner(owner.trim()));
        }
        return success(folderService.listMine());
    }

    @Operation(summary = "新建文件夹")
    @PreAuthorize("@ss.hasPermi('file:folder:add')")
    @AuditLog(module = "file.folder", action = "CREATE", resourceType = "folder",
            resourceId = "#result?.data?.id",
            comment = "'建文件夹 ' + #req.name")
    @PostMapping
    public AjaxResult add(@RequestBody FolderRequest req)
    {
        return success(folderService.create(req));
    }

    @Operation(summary = "改名文件夹")
    @PreAuthorize("@ss.hasPermi('file:folder:edit')")
    @AuditLog(module = "file.folder", action = "RENAME", resourceType = "folder",
            resourceId = "#req.id",
            comment = "'改名文件夹 ' + #req.id + ' -> ' + #req.name")
    @PutMapping
    public AjaxResult edit(@RequestBody FolderRequest req)
    {
        return success(folderService.rename(req));
    }

    @Operation(summary = "软删文件夹（递归子级）")
    @PreAuthorize("@ss.hasPermi('file:folder:remove')")
    @AuditLog(module = "file.folder", action = "REMOVE", resourceType = "folder",
            resourceId = "#id",
            comment = "'软删文件夹 ' + #id",
            recordReturn = false)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id)
    {
        folderService.remove(id);
        return success();
    }
}
