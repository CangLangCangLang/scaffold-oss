package com.scaffold.module.cms.controller;

import java.util.List;
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
import com.scaffold.module.cms.domain.Channel;
import com.scaffold.module.cms.dto.ChannelTreeNode;
import com.scaffold.module.cms.service.ChannelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * CMS 栏目（树）后台 API。<br>
 * 公开 API 走 {@link CmsPublicController}，无需 token。
 */
@Tag(name = "CMS 栏目管理（后台）", description = "栏目树 CRUD；删除前会校验子栏目 / 文章数量")
@RestController
@RequestMapping("/cms/channel")
public class CmsChannelController extends BaseController
{
    @Autowired private ChannelService channelService;

    @Operation(summary = "栏目列表（扁平）",
            description = "返回全部栏目（含停用 / 软删过滤后），不构造树形；前端用做下拉的全集")
    @PreAuthorize("@ss.hasPermi('cms:channel:list')")
    @GetMapping("/list")
    public AjaxResult list()
    {
        List<Channel> rows = channelService.list(null);
        return success(rows);
    }

    @Operation(summary = "栏目树",
            description = "返回栏目树结构，前端 el-tree 直渲；activeOnly=true 只返回 status='0' 启用的栏目（前台门户用）")
    @PreAuthorize("@ss.hasPermi('cms:channel:list')")
    @GetMapping("/tree")
    public AjaxResult tree(@Parameter(description = "true=只返回启用栏目；false=含停用，默认 false")
                           @RequestParam(required = false, defaultValue = "false") boolean activeOnly)
    {
        List<ChannelTreeNode> tree = channelService.tree(activeOnly);
        return success(tree);
    }

    @Operation(summary = "栏目详情")
    @PreAuthorize("@ss.hasPermi('cms:channel:list')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@Parameter(description = "栏目 id") @PathVariable Long id)
    {
        return success(channelService.getById(id));
    }

    @Operation(summary = "新建栏目",
            description = "code 全局唯一，会作为公开 API 的稳定 URL 标识；支持指定 parent_id 建子栏目")
    @PreAuthorize("@ss.hasPermi('cms:channel:add')")
    @AuditLog(module = "cms.channel", action = "CREATE", resourceType = "channel",
            resourceId = "#result?.data?.id",
            comment = "'新建栏目 ' + #form.name + ' (' + #form.code + ')'")
    @PostMapping
    public AjaxResult add(@RequestBody Channel form)
    {
        return success(channelService.create(form));
    }

    @Operation(summary = "编辑栏目",
            description = "禁止把栏目挂到自己的子孙下（防循环引用）；code 修改时仍校验唯一")
    @PreAuthorize("@ss.hasPermi('cms:channel:edit')")
    @AuditLog(module = "cms.channel", action = "UPDATE", resourceType = "channel",
            resourceId = "#form.id",
            comment = "'编辑栏目 ' + (#form.name ?: '?') + ' (id=' + #form.id + ')'")
    @PutMapping
    public AjaxResult edit(@RequestBody Channel form)
    {
        return success(channelService.update(form));
    }

    @Operation(summary = "删除栏目",
            description = "删除前会检查：栏目下是否还有子栏目 / 还有未软删文章；都没有才能删")
    @PreAuthorize("@ss.hasPermi('cms:channel:remove')")
    @AuditLog(module = "cms.channel", action = "DELETE", resourceType = "channel",
            resourceId = "#id",
            comment = "'删除栏目 id=' + #id",
            recordReturn = false)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@Parameter(description = "栏目 id") @PathVariable Long id)
    {
        channelService.delete(id);
        return success();
    }
}
