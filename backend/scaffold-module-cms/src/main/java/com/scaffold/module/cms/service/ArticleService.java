package com.scaffold.module.cms.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.scaffold.common.exception.ServiceException;
import com.scaffold.common.utils.SecurityUtils;
import com.scaffold.common.utils.StringUtils;
import com.scaffold.module.cms.domain.Article;
import com.scaffold.module.cms.domain.Tag;
import com.scaffold.module.cms.dto.ArticleQuery;
import com.scaffold.module.cms.dto.ArticleSaveRequest;
import com.scaffold.module.cms.dto.ArticleTagPair;
import com.scaffold.module.cms.event.ArticleStatusChangedEvent;
import com.scaffold.module.cms.mapper.ArticleMapper;
import com.scaffold.module.cms.mapper.ChannelMapper;
import com.scaffold.module.cms.mapper.TagMapper;
import com.scaffold.module.cms.workflow.CmsWorkflowAdapter;

/**
 * CMS 文章服务。<br>
 * 第 1 批：CRUD + 标签关联 + 软删 + 列表分页 + 详情。<br>
 * 第 2 批：6 个状态机流转动作（{@code submit/approve/reject/publish/unpublish/back-to-draft}）。<br>
 * 第 3 批（M-4 / M-5）：状态切换时发 {@link ArticleStatusChangedEvent}，
 * 让 cms-workflow / cms-inbox 等桥模块按需订阅；workflow 桥可在 {@link #submit} 中接管，
 * 也可通过 {@link #onWorkflowApprove(Long)} / {@link #onWorkflowReject(Long, String)} 反向同步。
 *
 * <h3>slug 生成约定</h3>
 * 用户传 {@code slug} 时严格按用户的来；只校验唯一性。<br>
 * 用户没传：按 {@code title} 生成 — 简体中文落到 NFD + 去除非 ASCII 后会被剃光，
 * 这种情况就回退成 {@code "article-<8 位 UUID>"}，保证总能唯一。
 */
@Service
public class ArticleService
{
    private static final Logger log = LoggerFactory.getLogger(ArticleService.class);

    private static final Pattern SLUG_NORMALIZE = Pattern.compile("[^\\p{Alnum}-]+");
    private static final Pattern SLUG_DEDUP = Pattern.compile("-{2,}");

    @Autowired private ArticleMapper articleMapper;
    @Autowired private ChannelMapper channelMapper;
    @Autowired private TagMapper tagMapper;
    @Autowired private CmsWorkflowAdapter workflowAdapter;
    @Autowired private ApplicationEventPublisher eventPublisher;

    /**
     * 后台分页列表。{@code pageNum / pageSize} 由调用方在 controller 里通过 {@code PageHelper.startPage}
     * 注入；这里只做查询与回填。
     */
    public PageInfo<Article> adminPage(ArticleQuery query, Integer pageNum, Integer pageSize)
    {
        if (query == null) query = new ArticleQuery();
        PageHelper.startPage(safe(pageNum, 1), safe(pageSize, 20));
        List<Article> rows = articleMapper.selectAdminList(query);
        fillTags(rows);
        return new PageInfo<>(rows);
    }

    public PageInfo<Article> publicPage(ArticleQuery query, Integer pageNum, Integer pageSize)
    {
        if (query == null) query = new ArticleQuery();
        query.setStatus(Article.STATUS_PUBLISHED);
        PageHelper.startPage(safe(pageNum, 1), safe(pageSize, 20));
        List<Article> rows = articleMapper.selectPublicList(query);
        fillTags(rows);
        return new PageInfo<>(rows);
    }

    public Article getById(Long id)
    {
        Article a = articleMapper.selectById(id);
        if (a == null) throw new ServiceException("文章不存在或已删除: " + id);
        a.setTagIds(articleMapper.selectTagIdsByArticleId(id));
        a.setTags(loadTagsByIds(a.getTagIds()));
        return a;
    }

    public Article getPublicBySlug(String slug)
    {
        Article a = articleMapper.selectPublicBySlug(slug);
        if (a == null) return null;
        a.setTagIds(articleMapper.selectTagIdsByArticleId(a.getId()));
        a.setTags(loadTagsByIds(a.getTagIds()));
        articleMapper.incrementViewCount(a.getId());
        return a;
    }

    @Transactional
    public Article save(ArticleSaveRequest req)
    {
        validateBase(req);
        if (req.getId() == null)
        {
            return doInsert(req);
        }
        return doUpdate(req);
    }

    @Transactional
    public void delete(Long id)
    {
        Article exist = getById(id);
        articleMapper.deleteTagsByArticleId(id);
        articleMapper.softDelete(id, SecurityUtils.getUsername());
        log.info("CMS 文章软删 id={} title={}", id, exist.getTitle());
    }

    /**
     * 通用状态切换；调用方需保证已经做过权限检查。<br>
     * {@code publishedAt} 仅在首次进入 PUBLISHED 时写入；下线再上线不重置。<br>
     * 状态切换成功后发 {@link ArticleStatusChangedEvent}（事务提交后由订阅端按需处理）。
     */
    @Transactional
    public Article setStatus(Long id, String targetStatus)
    {
        return setStatusInternal(id, targetStatus, null);
    }

    /**
     * 内部入口：可携带 reason，事件原样发出去（驳回 / 下线场景）。
     */
    @Transactional
    Article setStatusInternal(Long id, String targetStatus, String reason)
    {
        Article before = getById(id);
        validateTransition(before.getStatus(), targetStatus);
        java.util.Date publishedAt = before.getPublishedAt();
        if (Article.STATUS_PUBLISHED.equals(targetStatus) && publishedAt == null)
        {
            publishedAt = new java.util.Date();
        }
        articleMapper.updateStatus(id, targetStatus, publishedAt, SecurityUtils.getUsername());
        Article after = getById(id);
        publishStatusChanged(before, after, reason);
        return after;
    }

    /* ===== 状态机：6 个具名动作 ===== */

    /**
     * 提交审核：DRAFT → PENDING；如果存在外部 workflow 适配器接管，CMS 自身不再切状态，
     * 由 workflow 完成后回调 setStatus 写。
     */
    @Transactional
    public Article submit(Long id, String userId)
    {
        Article a = getById(id);
        if (!Article.STATUS_DRAFT.equals(a.getStatus()))
        {
            throw new ServiceException("仅草稿可提交审核，当前状态: " + a.getStatus());
        }
        boolean handed = false;
        try
        {
            handed = workflowAdapter.onSubmit(id, userId);
        }
        catch (Exception ex)
        {
            log.warn("CmsWorkflowAdapter.onSubmit 异常，回退自闭环 articleId={} reason={}", id, ex.getMessage());
        }
        if (handed)
        {
            // 桥模块已经在 onSubmit 内部完成了：startProcess + 回写 process_instance_id + 切状态到 PENDING
            // 这里只把"状态变化"事件补发出去，让其他订阅者（如 inbox）感知；
            // 注意桥模块自身不应再消费此事件——避免双重发送。
            Article after = getById(id);
            publishStatusChanged(a, after, null);
            log.info("CMS 文章已交由外部 workflow 处理 id={} actor={} piid={}",
                    id, userId, after.getProcessInstanceId());
            return after;
        }
        return setStatus(id, Article.STATUS_PENDING);
    }

    /** 审核通过：PENDING → PUBLISHED（首次发布写 publishedAt）。 */
    @Transactional
    public Article approve(Long id)
    {
        Article a = getById(id);
        if (!Article.STATUS_PENDING.equals(a.getStatus()))
        {
            throw new ServiceException("仅待审核可通过，当前状态: " + a.getStatus());
        }
        try { workflowAdapter.onApprove(id); }
        catch (Exception ex) { log.warn("workflowAdapter.onApprove 异常 id={} reason={}", id, ex.getMessage()); }
        return setStatus(id, Article.STATUS_PUBLISHED);
    }

    /**
     * 驳回：PENDING → DRAFT；reason 同时进 ApplicationEvent 与（由 controller 层）审计 comment。
     */
    @Transactional
    public Article reject(Long id, String reason)
    {
        Article a = getById(id);
        if (!Article.STATUS_PENDING.equals(a.getStatus()))
        {
            throw new ServiceException("仅待审核可驳回，当前状态: " + a.getStatus());
        }
        try { workflowAdapter.onReject(id, reason); }
        catch (Exception ex) { log.warn("workflowAdapter.onReject 异常 id={} reason={}", id, ex.getMessage()); }
        return setStatusInternal(id, Article.STATUS_DRAFT, reason);
    }

    /** 兼容老调用方：reject 不带 reason 时按 null 透传。 */
    @Transactional
    public Article reject(Long id) { return reject(id, null); }

    /** 下线：PUBLISHED → UNPUBLISHED；reason 进事件以便 inbox 桥发"已下线（原因 xxx）"。 */
    @Transactional
    public Article unpublish(Long id, String reason)
    {
        Article a = getById(id);
        if (!Article.STATUS_PUBLISHED.equals(a.getStatus()))
        {
            throw new ServiceException("仅已发布的文章可下线，当前状态: " + a.getStatus());
        }
        return setStatusInternal(id, Article.STATUS_UNPUBLISHED, reason);
    }

    /** 兼容老调用方：unpublish 不带 reason。 */
    @Transactional
    public Article unpublish(Long id) { return unpublish(id, null); }

    /** 重新上线：UNPUBLISHED → PUBLISHED；不重置首次发布时间。 */
    @Transactional
    public Article republish(Long id)
    {
        Article a = getById(id);
        if (!Article.STATUS_UNPUBLISHED.equals(a.getStatus()))
        {
            throw new ServiceException("仅已下线的文章可重新上线，当前状态: " + a.getStatus());
        }
        return setStatus(id, Article.STATUS_PUBLISHED);
    }

    /**
     * 退回到草稿：PENDING / PUBLISHED / UNPUBLISHED → DRAFT。<br>
     * 一般用于运营想撤回再编辑的场景；从已发布状态退回草稿前端会有"内容将下线"提示。
     */
    @Transactional
    public Article backToDraft(Long id)
    {
        Article a = getById(id);
        if (Article.STATUS_DRAFT.equals(a.getStatus()))
        {
            throw new ServiceException("当前已是草稿状态");
        }
        return setStatus(id, Article.STATUS_DRAFT);
    }

    /* ===== M-4 桥模块反向回调入口 ===== */

    /**
     * 桥模块从 Flowable {@code PROCESS_COMPLETED} 监听器中回调，文章切到 PUBLISHED。
     * 与 {@link #approve(Long)} 不同的是：这里不再调 workflowAdapter（避免回调闭环）。
     *
     * @param id 文章 id
     * @param actorUserId 完成此次审批的用户 id（来自 Flowable 任务的 assignee；事件 actor 用它）
     */
    @Transactional
    public Article onWorkflowApprove(Long id, String actorUserId)
    {
        Article before = getById(id);
        if (!Article.STATUS_PENDING.equals(before.getStatus()))
        {
            log.warn("onWorkflowApprove 跳过：文章状态非 PENDING id={} status={}", id, before.getStatus());
            return before;
        }
        java.util.Date publishedAt = before.getPublishedAt();
        if (publishedAt == null) publishedAt = new java.util.Date();
        articleMapper.updateStatus(id, Article.STATUS_PUBLISHED, publishedAt,
                actorUserId == null ? SecurityUtils.getUsername() : actorUserId);
        Article after = getById(id);
        publishStatusChanged(before, after, null, actorUserId);
        log.info("workflow → CMS 审核通过 id={} actor={} piid={}", id, actorUserId, after.getProcessInstanceId());
        return after;
    }

    /**
     * 桥模块从 Flowable 监听器中回调，文章驳回 PENDING → DRAFT。
     */
    @Transactional
    public Article onWorkflowReject(Long id, String reason, String actorUserId)
    {
        Article before = getById(id);
        if (!Article.STATUS_PENDING.equals(before.getStatus()))
        {
            log.warn("onWorkflowReject 跳过：文章状态非 PENDING id={} status={}", id, before.getStatus());
            return before;
        }
        articleMapper.updateStatus(id, Article.STATUS_DRAFT, before.getPublishedAt(),
                actorUserId == null ? SecurityUtils.getUsername() : actorUserId);
        Article after = getById(id);
        publishStatusChanged(before, after, reason, actorUserId);
        log.info("workflow → CMS 审核驳回 id={} actor={} reason={} piid={}",
                id, actorUserId, reason, after.getProcessInstanceId());
        return after;
    }

    /* ===================== 私有 ===================== */

    private void publishStatusChanged(Article before, Article after, String reason)
    {
        publishStatusChanged(before, after, reason, SecurityUtils.getUsername());
    }

    private void publishStatusChanged(Article before, Article after, String reason, String actorUserId)
    {
        try
        {
            ArticleStatusChangedEvent event = new ArticleStatusChangedEvent(
                    this,
                    after.getId(),
                    after.getTitle(),
                    after.getChannelId(),
                    before == null ? null : before.getStatus(),
                    after.getStatus(),
                    actorUserId,
                    after.getCreateBy(),
                    reason,
                    after.getProcessInstanceId());
            eventPublisher.publishEvent(event);
        }
        catch (Exception ex)
        {
            // 事件发送失败不应影响主业务
            log.warn("发送 ArticleStatusChangedEvent 失败 id={} reason={}", after.getId(), ex.getMessage());
        }
    }

    private Article doInsert(ArticleSaveRequest req)
    {
        Article a = new Article();
        copyEditable(req, a);
        a.setStatus(Article.STATUS_DRAFT);
        a.setViewCount(0L);
        a.setCreateBy(SecurityUtils.getUsername());

        String slug = StringUtils.isNotEmpty(req.getSlug()) ? req.getSlug().trim()
                                                           : generateSlug(a.getTitle());
        a.setSlug(ensureSlugUnique(slug, null));

        articleMapper.insert(a);
        replaceTags(a.getId(), req.getTagIds());
        log.info("CMS 文章已创建 id={} title={} slug={}", a.getId(), a.getTitle(), a.getSlug());
        return getById(a.getId());
    }

    private Article doUpdate(ArticleSaveRequest req)
    {
        Article exist = getById(req.getId());
        Article a = new Article();
        a.setId(exist.getId());
        copyEditable(req, a);
        a.setUpdateBy(SecurityUtils.getUsername());

        if (StringUtils.isNotEmpty(req.getSlug()) && !req.getSlug().equals(exist.getSlug()))
        {
            a.setSlug(ensureSlugUnique(req.getSlug().trim(), exist.getId()));
        }

        articleMapper.updateById(a);
        replaceTags(a.getId(), req.getTagIds());
        return getById(a.getId());
    }

    private static void copyEditable(ArticleSaveRequest src, Article dst)
    {
        dst.setChannelId(src.getChannelId());
        dst.setTitle(src.getTitle().trim());
        dst.setSummary(StringUtils.isEmpty(src.getSummary()) ? "" : src.getSummary());
        dst.setCoverUrl(StringUtils.isEmpty(src.getCoverUrl()) ? "" : src.getCoverUrl());
        dst.setContentHtml(src.getContentHtml());
        dst.setSource(StringUtils.isEmpty(src.getSource()) ? "" : src.getSource());
        dst.setAuthor(StringUtils.isEmpty(src.getAuthor()) ? "" : src.getAuthor());
        dst.setMetaTitle(StringUtils.isEmpty(src.getMetaTitle()) ? "" : src.getMetaTitle());
        dst.setMetaDescription(StringUtils.isEmpty(src.getMetaDescription()) ? "" : src.getMetaDescription());
        dst.setMetaKeywords(StringUtils.isEmpty(src.getMetaKeywords()) ? "" : src.getMetaKeywords());
        dst.setCanonicalUrl(StringUtils.isEmpty(src.getCanonicalUrl()) ? "" : src.getCanonicalUrl());
        dst.setSortOrder(src.getSortOrder() == null ? 0 : src.getSortOrder());
    }

    private void validateBase(ArticleSaveRequest req)
    {
        if (req == null) throw new ServiceException("请求体为空");
        if (StringUtils.isEmpty(req.getTitle())) throw new ServiceException("标题不能为空");
        if (req.getChannelId() == null) throw new ServiceException("必须选择栏目");
        if (channelMapper.selectById(req.getChannelId()) == null)
        {
            throw new ServiceException("栏目不存在或已删除: " + req.getChannelId());
        }
    }

    /** 状态机校验。第 2 批会被 ArticleStateMachine 替换。 */
    static void validateTransition(String from, String to)
    {
        if (from == null || to == null) throw new ServiceException("status 不能为空");
        if (Objects.equals(from, to)) return;
        boolean ok = switch (from)
        {
            case Article.STATUS_DRAFT -> to.equals(Article.STATUS_PENDING);
            case Article.STATUS_PENDING -> to.equals(Article.STATUS_PUBLISHED) || to.equals(Article.STATUS_DRAFT);
            case Article.STATUS_PUBLISHED -> to.equals(Article.STATUS_UNPUBLISHED) || to.equals(Article.STATUS_DRAFT);
            case Article.STATUS_UNPUBLISHED -> to.equals(Article.STATUS_PUBLISHED) || to.equals(Article.STATUS_DRAFT);
            default -> false;
        };
        if (!ok) throw new ServiceException("非法状态流转: " + from + " -> " + to);
    }

    private void replaceTags(Long articleId, List<Long> tagIds)
    {
        articleMapper.deleteTagsByArticleId(articleId);
        if (tagIds == null || tagIds.isEmpty()) return;
        List<Long> distinct = tagIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) return;
        articleMapper.batchInsertTags(articleId, distinct);
    }

    private void fillTags(List<Article> rows)
    {
        if (rows == null || rows.isEmpty()) return;
        Set<Long> articleIds = new HashSet<>();
        for (Article a : rows) articleIds.add(a.getId());
        List<ArticleTagPair> pairs = articleMapper.selectArticleTagPairs(articleIds);
        Map<Long, List<Long>> byArticle = new LinkedHashMap<>();
        Set<Long> tagIds = new HashSet<>();
        for (ArticleTagPair p : pairs)
        {
            byArticle.computeIfAbsent(p.getArticleId(), k -> new ArrayList<>()).add(p.getTagId());
            tagIds.add(p.getTagId());
        }
        Map<Long, Tag> tagMap = new LinkedHashMap<>();
        if (!tagIds.isEmpty())
        {
            for (Tag t : tagMapper.selectByIds(tagIds))
            {
                tagMap.put(t.getId(), t);
            }
        }
        for (Article a : rows)
        {
            List<Long> ids = byArticle.getOrDefault(a.getId(), Collections.emptyList());
            a.setTagIds(ids);
            List<Tag> tags = new ArrayList<>(ids.size());
            for (Long id : ids)
            {
                Tag t = tagMap.get(id);
                if (t != null) tags.add(t);
            }
            a.setTags(tags);
        }
    }

    private List<Tag> loadTagsByIds(List<Long> ids)
    {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        return tagMapper.selectByIds(new HashSet<>(ids));
    }

    /** 生成 slug：拉丁字符 + 数字 + '-'，其余压缩。中文等非 ASCII 会被剔光，那时回退成 article-<uuid8>。 */
    static String generateSlug(String title)
    {
        if (title == null) title = "";
        String norm = Normalizer.normalize(title, Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]+", "")
                .toLowerCase().trim();
        norm = SLUG_NORMALIZE.matcher(norm).replaceAll("-");
        norm = SLUG_DEDUP.matcher(norm).replaceAll("-");
        norm = norm.replaceAll("^-|-$", "");
        if (norm.isEmpty())
        {
            norm = "article-" + UUID.randomUUID().toString().substring(0, 8);
        }
        if (norm.length() > 150) norm = norm.substring(0, 150);
        return norm;
    }

    /** 给 base slug 找一个唯一的：base / base-2 / base-3 ... 直到不冲突。selfId 用来排除自己。 */
    private String ensureSlugUnique(String base, Long selfId)
    {
        Article exist = articleMapper.selectBySlug(base);
        if (exist == null || (selfId != null && exist.getId().equals(selfId))) return base;
        for (int i = 2; i < 1000; i++)
        {
            String candidate = base + "-" + i;
            Article hit = articleMapper.selectBySlug(candidate);
            if (hit == null || (selfId != null && hit.getId().equals(selfId))) return candidate;
        }
        throw new ServiceException("slug 生成冲突过多，请显式指定: " + base);
    }

    private static int safe(Integer v, int def) { return (v == null || v <= 0) ? def : v; }
}
