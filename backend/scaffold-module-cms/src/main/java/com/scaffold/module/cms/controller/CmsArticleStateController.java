package com.scaffold.module.cms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.scaffold.common.annotation.AuditLog;
import com.scaffold.common.core.controller.BaseController;
import com.scaffold.common.core.domain.AjaxResult;
import com.scaffold.module.cms.dto.ArticleStateChangeRequest;
import com.scaffold.module.cms.service.ArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * CMS 文章状态机端点（与 {@link CmsArticleController} 分开，权限独立 + 审计粒度更细）。
 * <p>
 * 流转规则：
 * <ul>
 *   <li>POST /cms/article/{id}/submit       —— DRAFT → PENDING</li>
 *   <li>POST /cms/article/{id}/approve      —— PENDING → PUBLISHED</li>
 *   <li>POST /cms/article/{id}/reject       —— PENDING → DRAFT (reason 落审计)</li>
 *   <li>POST /cms/article/{id}/publish      —— UNPUBLISHED → PUBLISHED (重新上线)</li>
 *   <li>POST /cms/article/{id}/unpublish    —— PUBLISHED → UNPUBLISHED</li>
 *   <li>POST /cms/article/{id}/back-to-draft —— PENDING/PUBLISHED/UNPUBLISHED → DRAFT (兜底退回)</li>
 * </ul>
 */
@Tag(name = "CMS 文章状态机", description = "submit / approve / reject / publish / unpublish / back-to-draft 六个流转端点；M-4 桥启用时 submit 走 Flowable 真审批")
@RestController
@RequestMapping("/cms/article")
public class CmsArticleStateController extends BaseController
{
    @Autowired private ArticleService articleService;

    @Operation(summary = "提交审核",
            description = "DRAFT → PENDING；M-4 cms-workflow 桥启用时会启动 cms_article_review 流程并把 piid 写到 cms_article.process_instance_id")
    @PreAuthorize("@ss.hasPermi('cms:article:submit')")
    @AuditLog(module = "cms.article", action = "SUBMIT", resourceType = "article",
            resourceId = "#id",
            comment = "'提交审核 id=' + #id + (#req?.reason != null ? ' reason=' + #req.reason : '')",
            recordReturn = false)
    @PostMapping("/{id}/submit")
    public AjaxResult submit(@Parameter(description = "文章 id") @PathVariable Long id,
                             @RequestBody(required = false) ArticleStateChangeRequest req)
    {
        return success(articleService.submit(id, String.valueOf(getUserId())));
    }

    @Operation(summary = "审核通过",
            description = "PENDING → PUBLISHED；首次进入 PUBLISHED 时写 published_at；当文章已绑 workflow piid 时会同步 cancelInstance 清掉 Flowable 上挂着的待办")
    @PreAuthorize("@ss.hasPermi('cms:article:approve')")
    @AuditLog(module = "cms.article", action = "APPROVE", resourceType = "article",
            resourceId = "#id",
            comment = "'审核通过 id=' + #id",
            recordReturn = false)
    @PostMapping("/{id}/approve")
    public AjaxResult approve(@Parameter(description = "文章 id") @PathVariable Long id,
                              @RequestBody(required = false) ArticleStateChangeRequest req)
    {
        return success(articleService.approve(id));
    }

    @Operation(summary = "审核驳回",
            description = "PENDING → DRAFT；reason 必传，会落到 sys_audit_log 与作者站内信（M-5 cms-inbox）")
    @PreAuthorize("@ss.hasPermi('cms:article:approve')")
    @AuditLog(module = "cms.article", action = "REJECT", resourceType = "article",
            resourceId = "#id",
            comment = "'审核驳回 id=' + #id + (#req?.reason != null ? ' reason=' + #req.reason : '')",
            recordReturn = false)
    @PostMapping("/{id}/reject")
    public AjaxResult reject(@Parameter(description = "文章 id") @PathVariable Long id,
                             @RequestBody(required = false) ArticleStateChangeRequest req)
    {
        return success(articleService.reject(id, req == null ? null : req.getReason()));
    }

    @Operation(summary = "重新上线",
            description = "UNPUBLISHED → PUBLISHED；published_at 不重置（保留首发时间语义）")
    @PreAuthorize("@ss.hasPermi('cms:article:publish')")
    @AuditLog(module = "cms.article", action = "REPUBLISH", resourceType = "article",
            resourceId = "#id",
            comment = "'重新上线 id=' + #id",
            recordReturn = false)
    @PostMapping("/{id}/publish")
    public AjaxResult republish(@Parameter(description = "文章 id") @PathVariable Long id,
                                @RequestBody(required = false) ArticleStateChangeRequest req)
    {
        return success(articleService.republish(id));
    }

    @Operation(summary = "下线文章",
            description = "PUBLISHED → UNPUBLISHED；body 中可携带 reason 进审计与作者站内信")
    @PreAuthorize("@ss.hasPermi('cms:article:unpublish')")
    @AuditLog(module = "cms.article", action = "UNPUBLISH", resourceType = "article",
            resourceId = "#id",
            comment = "'文章下线 id=' + #id + (#req?.reason != null ? ' reason=' + #req.reason : '')",
            recordReturn = false)
    @PostMapping("/{id}/unpublish")
    public AjaxResult unpublish(@Parameter(description = "文章 id") @PathVariable Long id,
                                @RequestBody(required = false) ArticleStateChangeRequest req)
    {
        return success(articleService.unpublish(id, req == null ? null : req.getReason()));
    }

    @Operation(summary = "兜底退回草稿",
            description = "PENDING / PUBLISHED / UNPUBLISHED → DRAFT；用于「误操作」或运营拉回再编辑场景")
    @PreAuthorize("@ss.hasPermi('cms:article:edit')")
    @AuditLog(module = "cms.article", action = "BACK_TO_DRAFT", resourceType = "article",
            resourceId = "#id",
            comment = "'退回草稿 id=' + #id + (#req?.reason != null ? ' reason=' + #req.reason : '')",
            recordReturn = false)
    @PostMapping("/{id}/back-to-draft")
    public AjaxResult backToDraft(@Parameter(description = "文章 id") @PathVariable Long id,
                                  @RequestBody(required = false) ArticleStateChangeRequest req)
    {
        return success(articleService.backToDraft(id));
    }
}
