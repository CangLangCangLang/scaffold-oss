package com.scaffold.module.cms.controller;

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
import com.scaffold.module.cms.domain.Tag;
import com.scaffold.module.cms.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

@io.swagger.v3.oas.annotations.tags.Tag(name = "CMS 标签字典", description = "标签 CRUD；删标签会同步清掉 article-tag 关联，不留孤儿")
@RestController
@RequestMapping("/cms/tag")
public class CmsTagController extends BaseController
{
    @Autowired private TagService tagService;

    @Operation(summary = "标签列表", description = "name 传入时按 LIKE 模糊匹配；不传返回全量")
    @PreAuthorize("@ss.hasPermi('cms:tag:list')")
    @GetMapping("/list")
    public AjaxResult list(@Parameter(description = "标签名关键字，LIKE %name%") @RequestParam(required = false) String name)
    {
        return success(tagService.list(name));
    }

    @Operation(summary = "标签详情")
    @PreAuthorize("@ss.hasPermi('cms:tag:list')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@Parameter(description = "标签 id") @PathVariable Long id)
    {
        return success(tagService.getById(id));
    }

    @Operation(summary = "新建标签", description = "name 唯一，色值 color 可选（前端列表展示用）")
    @PreAuthorize("@ss.hasPermi('cms:tag:add')")
    @AuditLog(module = "cms.tag", action = "CREATE", resourceType = "tag",
            resourceId = "#result?.data?.id",
            comment = "'新建标签 ' + #form.name")
    @PostMapping
    public AjaxResult add(@RequestBody Tag form)
    {
        return success(tagService.create(form));
    }

    @Operation(summary = "编辑标签")
    @PreAuthorize("@ss.hasPermi('cms:tag:edit')")
    @AuditLog(module = "cms.tag", action = "UPDATE", resourceType = "tag",
            resourceId = "#form.id",
            comment = "'编辑标签 id=' + #form.id")
    @PutMapping
    public AjaxResult edit(@RequestBody Tag form)
    {
        return success(tagService.update(form));
    }

    @Operation(summary = "删除标签", description = "硬删标签 + 关联表对应记录；不影响文章本身")
    @PreAuthorize("@ss.hasPermi('cms:tag:remove')")
    @AuditLog(module = "cms.tag", action = "DELETE", resourceType = "tag",
            resourceId = "#id",
            comment = "'删除标签 id=' + #id",
            recordReturn = false)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@Parameter(description = "标签 id") @PathVariable Long id)
    {
        tagService.delete(id);
        return success();
    }
}
