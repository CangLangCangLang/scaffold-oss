package com.scaffold.module.file.controller;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.scaffold.common.annotation.AuditLog;
import com.scaffold.common.core.controller.BaseController;
import com.scaffold.common.core.domain.AjaxResult;
import com.scaffold.common.core.page.TableDataInfo;
import com.scaffold.common.utils.SecurityUtils;
import com.scaffold.module.file.dto.FileEditRequest;
import com.scaffold.module.file.dto.FileQuery;
import com.scaffold.module.file.service.FileRefService;
import com.scaffold.module.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 文件中心：文件 CRUD + 引用查询（M-6）。
 *
 * <p>权限模型：{@code file:list} 可看全量；{@code file:list:mine} 强制按 createBy=current 过滤。
 *
 * @author scaffold
 */
@Tag(name = "文件中心 - 文件（M-6）", description = "上传记录主表 CRUD + 引用计数管理 + 软删 / 硬删 / 立即清回收站")
@RestController
@RequestMapping("/file/file")
public class FileController extends BaseController
{
    @Autowired private FileService fileService;
    @Autowired private FileRefService fileRefService;

    @Operation(summary = "分页查询文件",
            description = "拥有 file:list 看全量；只有 file:list:mine 时强制按当前用户过滤")
    @PreAuthorize("@ss.hasAnyPermi('file:list,file:list:mine')")
    @GetMapping
    public TableDataInfo list(@ModelAttribute FileQuery q)
    {
        boolean canSeeAll = SecurityUtils.hasPermi("file:list");
        if (!canSeeAll)
        {
            q.setCreateBy(SecurityUtils.getUsername());
        }
        Map<String, Object> p = fileService.page(q);
        TableDataInfo info = new TableDataInfo();
        info.setRows((List<?>) p.get("rows"));
        info.setTotal((long) p.get("total"));
        info.setCode(200);
        info.setMsg("查询成功");
        return info;
    }

    @Operation(summary = "查看文件详情")
    @PreAuthorize("@ss.hasAnyPermi('file:list,file:list:mine')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id)
    {
        return success(fileService.detail(id));
    }

    @Operation(summary = "上传文件",
            description = "走 framework UploadStorageService（默认白名单 + 默认 10MB 上限）；返回 sys_file 主记录")
    @PreAuthorize("@ss.hasPermi('file:file:upload')")
    @AuditLog(module = "file.file", action = "UPLOAD", resourceType = "file",
            resourceId = "#result?.data?.id",
            comment = "'上传文件 ' + (#file != null ? #file.originalFilename : '?')")
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file,
                             @Parameter(description = "业务桶（默认 common）") @RequestParam(value = "bucket", required = false) String bucket,
                             @Parameter(description = "目标文件夹 ID（默认根）") @RequestParam(value = "folderId", required = false) Long folderId)
    {
        return success(fileService.upload(file, bucket, folderId));
    }

    @Operation(summary = "改名 / 移动文件夹 / 改分类标签")
    @PreAuthorize("@ss.hasPermi('file:file:edit')")
    @AuditLog(module = "file.file", action = "EDIT", resourceType = "file",
            resourceId = "#req.id",
            comment = "'编辑文件 ' + #req.id")
    @PutMapping
    public AjaxResult edit(@RequestBody FileEditRequest req)
    {
        return success(fileService.edit(req));
    }

    @Operation(summary = "软删（30 天后由 quartz 物理清理；ref_count>0 拒删）")
    @PreAuthorize("@ss.hasPermi('file:file:remove')")
    @AuditLog(module = "file.file", action = "REMOVE", resourceType = "file",
            resourceId = "#id",
            comment = "'软删文件 ' + #id",
            recordReturn = false)
    @DeleteMapping("/{id}")
    public AjaxResult softRemove(@PathVariable Long id)
    {
        fileService.softRemove(id);
        return success();
    }

    @Operation(summary = "批量软删")
    @PreAuthorize("@ss.hasPermi('file:file:batch-remove')")
    @AuditLog(module = "file.file", action = "BATCH_REMOVE", resourceType = "file",
            comment = "'批量软删 ' + #ids?.size() + ' 个文件'",
            recordReturn = false)
    @DeleteMapping("/batch")
    public AjaxResult batchSoftRemove(@RequestBody List<Long> ids)
    {
        return success(fileService.batchSoftRemove(ids));
    }

    @Operation(summary = "立即清回收站（管理员，绕开 30 天等待）",
            description = "彻底清盘 + 删 DB；只能用于已软删的文件；ref_count>0 仍拒")
    @PreAuthorize("@ss.hasPermi('file:file:purge')")
    @AuditLog(module = "file.file", action = "PURGE", resourceType = "file",
            resourceId = "#id",
            comment = "'立即清盘 ' + #id",
            recordReturn = false)
    @DeleteMapping("/purge/{id}")
    public AjaxResult hardRemove(@PathVariable Long id)
    {
        return success(fileService.hardRemove(id));
    }

    @Operation(summary = "手动触发软删超 30 天清磁盘任务",
            description = "脚手架未启用 quartz 时备用；同 quartz 任务等价")
    @PreAuthorize("@ss.hasPermi('file:file:purge')")
    @AuditLog(module = "file.file", action = "PURGE_NOW", resourceType = "file",
            comment = "'手动触发清磁盘任务'")
    @PostMapping("/purge-now")
    public AjaxResult purgeNow(@Parameter(description = "保留天数；默认 30") @RequestParam(value = "retainDays", required = false) Integer retainDays)
    {
        int days = (retainDays == null || retainDays <= 0) ? FileService.DEFAULT_RETAIN_DAYS : retainDays;
        int n = fileService.purgeExpired(days);
        return success(Map.of("purged", n, "retainDays", days));
    }

    @Operation(summary = "查看本文件被哪些业务模块引用",
            description = "返回 sys_file_ref 列表（cms / form / wf 等）；前端给删除按钮做提示用")
    @PreAuthorize("@ss.hasAnyPermi('file:list,file:list:mine')")
    @GetMapping("/{id}/refs")
    public AjaxResult refs(@PathVariable Long id)
    {
        return success(fileRefService.listByFile(id));
    }
}
