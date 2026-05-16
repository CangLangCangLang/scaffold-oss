package com.scaffold.module.cms.controller;

import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.scaffold.common.annotation.Anonymous;
import com.scaffold.common.constant.HttpStatus;
import com.scaffold.common.core.controller.BaseController;
import com.scaffold.common.core.domain.AjaxResult;
import com.scaffold.common.core.page.TableDataInfo;
import com.scaffold.module.cms.domain.Article;
import com.scaffold.module.cms.dto.ArticleQuery;
import com.scaffold.module.cms.dto.ChannelTreeNode;
import com.scaffold.module.cms.service.ArticleService;
import com.scaffold.module.cms.service.ChannelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * CMS 公开门户 API（匿名访问，{@link Anonymous}）。
 * <ul>
 *   <li>GET /cms/public/channels —— 仅返回 status='0' 且 del_flag='0' 的栏目树</li>
 *   <li>GET /cms/public/articles —— 仅返回 status='PUBLISHED' 且 del_flag='0' 的文章列表（分页）</li>
 *   <li>GET /cms/public/articles/{slug} —— 同上 + slug 唯一匹配；命中时阅读量 +1</li>
 * </ul>
 * 阅读量更新由 {@link ArticleService#getPublicBySlug} 在事务里自加，幂等性靠唯一索引保证。
 */
@Tag(name = "CMS 公开门户（匿名）", description = "对外资讯门户：仅返回 PUBLISHED 文章 + 启用栏目；不需要 token")
@RestController
@RequestMapping("/cms/public")
@Anonymous
public class CmsPublicController extends BaseController
{
    @Autowired private ChannelService channelService;
    @Autowired private ArticleService articleService;

    @Operation(summary = "公开栏目树",
            description = "仅返回 status='0' 启用的栏目，过滤软删；前端门户做导航用")
    @GetMapping("/channels")
    public AjaxResult channels()
    {
        java.util.List<ChannelTreeNode> tree = channelService.tree(true);
        return success(tree);
    }

    @Operation(summary = "公开文章分页",
            description = "仅返回 status='PUBLISHED' 文章；列表不含 contentHtml（节省体积），按 published_at desc 排序")
    @GetMapping("/articles")
    public TableDataInfo articles(ArticleQuery query,
                                  @Parameter(description = "页码，1 起步") @RequestParam(required = false) Integer pageNum,
                                  @Parameter(description = "每页大小，默认 10") @RequestParam(required = false) Integer pageSize)
    {
        PageInfo<Article> page = articleService.publicPage(query, pageNum, pageSize);
        TableDataInfo info = new TableDataInfo();
        info.setRows(page.getList());
        info.setTotal(page.getTotal());
        info.setCode(HttpStatus.SUCCESS);
        info.setMsg("查询成功");
        return info;
    }

    @Operation(summary = "公开文章详情（按 slug）",
            description = "命中时事务内 view_count +=1（不会重复计数）；slug 不存在或文章非 PUBLISHED 返回 404")
    @GetMapping("/articles/{slug}")
    public AjaxResult articleBySlug(@Parameter(description = "文章 slug，公开访问的稳定 URL 段") @PathVariable String slug)
    {
        Article a = articleService.getPublicBySlug(slug);
        if (a == null)
        {
            return AjaxResult.error(HttpStatus.NOT_FOUND, "文章不存在或未发布");
        }
        return success(a);
    }
}
