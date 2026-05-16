package com.scaffold.module.cms.controller;

import com.github.pagehelper.PageInfo;
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
import com.scaffold.common.constant.HttpStatus;
import com.scaffold.common.core.controller.BaseController;
import com.scaffold.common.core.domain.AjaxResult;
import com.scaffold.common.core.page.TableDataInfo;
import com.scaffold.module.cms.domain.Article;
import com.scaffold.module.cms.dto.ArticleQuery;
import com.scaffold.module.cms.dto.ArticleSaveRequest;
import com.scaffold.module.cms.service.ArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * CMS 文章后台 API。<br>
 * 第 1 批仅做 CRUD + 列表 + 软删；状态机流转端点（submit/approve/...）放在第 2 批的
 * {@code CmsArticleStateController} 上。
 */
@Tag(name = "CMS 文章管理（后台）", description = "栏目内文章 CRUD、列表分页与搜索；状态机流转见 CmsArticleStateController")
@RestController
@RequestMapping("/cms/article")
public class CmsArticleController extends BaseController
{
    @Autowired private ArticleService articleService;

    @Operation(summary = "分页查询文章（后台）",
            description = "支持按 channelId / status / keyword（标题、摘要、正文 LIKE）/ tagId 过滤，含全部状态")
    @PreAuthorize("@ss.hasPermi('cms:article:list')")
    @GetMapping("/list")
    public TableDataInfo list(ArticleQuery query,
                              @Parameter(description = "页码，1 起步；不传走 PageHelper 默认") @RequestParam(required = false) Integer pageNum,
                              @Parameter(description = "每页大小，默认 10") @RequestParam(required = false) Integer pageSize)
    {
        PageInfo<Article> page = articleService.adminPage(query, pageNum, pageSize);
        TableDataInfo info = new TableDataInfo();
        info.setRows(page.getList());
        info.setTotal(page.getTotal());
        info.setCode(HttpStatus.SUCCESS);
        info.setMsg("查询成功");
        return info;
    }

    @Operation(summary = "查询文章详情",
            description = "返回 contentHtml + tagIds + tags；不区分状态，软删的文章拿 404")
    @PreAuthorize("@ss.hasPermi('cms:article:list')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@Parameter(description = "文章 id") @PathVariable Long id)
    {
        return success(articleService.getById(id));
    }

    @Operation(summary = "新建文章",
            description = "默认 status=DRAFT；slug 不传则按 title 自动生成 + 唯一化（中文降级走 article-{8 位 UUID}）")
    @PreAuthorize("@ss.hasPermi('cms:article:add')")
    @AuditLog(module = "cms.article", action = "CREATE", resourceType = "article",
            resourceId = "#result?.data?.id",
            comment = "'新建文章 ' + #form.title",
            excludeFields = {"contentHtml"})
    @PostMapping
    public AjaxResult add(@RequestBody ArticleSaveRequest form)
    {
        form.setId(null);
        return success(articleService.save(form));
    }

    @Operation(summary = "编辑文章",
            description = "form.id 必填；任何状态都能编辑（已发布的会留前端上下线提示）")
    @PreAuthorize("@ss.hasPermi('cms:article:edit')")
    @AuditLog(module = "cms.article", action = "UPDATE", resourceType = "article",
            resourceId = "#form.id",
            comment = "'编辑文章 id=' + #form.id",
            excludeFields = {"contentHtml"})
    @PutMapping
    public AjaxResult edit(@RequestBody ArticleSaveRequest form)
    {
        if (form.getId() == null) return AjaxResult.error("缺少 id");
        return success(articleService.save(form));
    }

    @Operation(summary = "软删文章",
            description = "逻辑删除（del_flag=2），不真删；同时清掉 article-tag 关联，公开 API 立即不可见")
    @PreAuthorize("@ss.hasPermi('cms:article:remove')")
    @AuditLog(module = "cms.article", action = "DELETE", resourceType = "article",
            resourceId = "#id",
            comment = "'软删文章 id=' + #id",
            recordReturn = false)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@Parameter(description = "文章 id") @PathVariable Long id)
    {
        articleService.delete(id);
        return success();
    }
}
