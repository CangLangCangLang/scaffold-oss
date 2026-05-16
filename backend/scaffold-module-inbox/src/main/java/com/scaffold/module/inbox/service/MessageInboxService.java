package com.scaffold.module.inbox.service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.framework.web.websocket.bus.PushMessage;
import com.scaffold.module.inbox.domain.MessageInboxEntry;
import com.scaffold.module.inbox.dto.InboxQueryRequest;
import com.scaffold.module.inbox.mapper.MessageInboxMapper;

/**
 * 离线消息盒服务：
 * <ul>
 *   <li>{@link #persistIfEnabled(PushMessage)} 在 Redis fan-out 之前同步落库，
 *       即使所有节点没人在线、消息也能等用户上线后被读取。</li>
 *   <li>{@link #fetchUnread(String, int)} 用户上线 / WebSocket 重连时被前端调用拉取未读。</li>
 *   <li>{@link #ack(Long, String)} 与 {@link #ackAll(String)} 维护已读状态。</li>
 *   <li>{@link #cleanupExpired(int)} 由 Quartz 定时调用清理过期数据。</li>
 * </ul>
 *
 * @author scaffold
 */
@Service
public class MessageInboxService
{
    private static final Logger log = LoggerFactory.getLogger(MessageInboxService.class);

    private final MessageInboxMapper inboxMapper;
    private final ObjectMapper objectMapper;

    @Value("${inbox.persist:true}")
    private boolean persist;

    @Value("${inbox.default-ttl-seconds:604800}")
    private long defaultTtlSeconds;

    public MessageInboxService(MessageInboxMapper inboxMapper, ObjectMapper objectMapper)
    {
        this.inboxMapper = inboxMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 在 fan-out 之前把消息记一笔；返回 inbox.id 给上层（如需）。
     * 当配置 {@code inbox.persist=false} 时静默退化为不落库。
     */
    @Transactional
    public Long persistIfEnabled(PushMessage message)
    {
        if (!persist || message == null) return null;
        if (message.getScope() == null || message.getTarget() == null) return null;
        // TOPIC 消息不入库（每个订阅者独立维护已读状态需要展开为 N 行，规模复杂；按需扩展）
        if (message.getScope() != PushMessage.Scope.USER) return null;

        MessageInboxEntry entry = new MessageInboxEntry();
        entry.setMessageId(message.getId());
        entry.setScope(message.getScope().name());
        entry.setTarget(message.getTarget());
        entry.setType(message.getType());
        try
        {
            entry.setPayload(message.getPayload() == null ? null
                    : objectMapper.writeValueAsString(message.getPayload()));
        }
        catch (JsonProcessingException e)
        {
            log.warn("inbox 序列化 payload 失败 type={} reason={}", message.getType(), e.getMessage());
        }
        entry.setStatus(0);
        entry.setCreatedAt(new Date());
        if (defaultTtlSeconds > 0)
        {
            entry.setExpireAt(new Date(System.currentTimeMillis() + defaultTtlSeconds * 1000L));
        }
        try
        {
            inboxMapper.insert(entry);
            return entry.getId();
        }
        catch (Exception ex)
        {
            // 唯一键冲突（messageId 重发）忽略；其余只 warn，不影响 fan-out
            log.warn("inbox 落库失败 type={} target={} reason={}",
                    message.getType(), message.getTarget(), ex.getMessage());
            return null;
        }
    }

    public List<MessageInboxEntry> fetchUnread(String username, int limit)
    {
        if (username == null || username.isEmpty()) return Collections.emptyList();
        return inboxMapper.selectUnreadByUser(username, Math.max(1, limit));
    }

    @Transactional
    public boolean ack(Long inboxId, String username)
    {
        if (inboxId == null || username == null) return false;
        return inboxMapper.markRead(inboxId, username) > 0;
    }

    @Transactional
    public int ackAll(String username)
    {
        return username == null ? 0 : inboxMapper.markAllReadByUser(username);
    }

    public int countUnread(String username)
    {
        return username == null ? 0 : inboxMapper.countUnreadByUser(username);
    }

    /**
     * 全页面分页查询：按用户范围 + 状态 + 类型 LIKE + 时间窗口过滤。
     * 跨用户隔离：username 由 controller 从 SecurityUtils 取，禁止从入参读取。
     */
    public PageResult<MessageInboxEntry> page(String username, InboxQueryRequest req)
    {
        if (username == null || username.isEmpty() || req == null)
        {
            return new PageResult<>(Collections.emptyList(), 0L);
        }
        int offset = (req.getPageNum() - 1) * req.getPageSize();
        long total = inboxMapper.countPageByUser(username, req.getStatuses(), req.getTypeKeyword(),
                req.getFromTime(), req.getToTime());
        if (total == 0)
        {
            return new PageResult<>(Collections.emptyList(), 0L);
        }
        List<MessageInboxEntry> rows = inboxMapper.selectPageByUser(username, req.getStatuses(),
                req.getTypeKeyword(), req.getFromTime(), req.getToTime(), offset, req.getPageSize());
        return new PageResult<>(rows, total);
    }

    /**
     * 批量标记已读：仅本人的未读消息会被命中（强制 status=0 + target=username 过滤）。
     */
    @Transactional
    public int ackBatch(String username, List<Long> ids)
    {
        if (username == null || ids == null || ids.isEmpty()) return 0;
        return inboxMapper.markBatchReadByIds(username, ids);
    }

    /**
     * 批量物理删除：仅本人记录会被命中。
     */
    @Transactional
    public int removeBatch(String username, List<Long> ids)
    {
        if (username == null || ids == null || ids.isEmpty()) return 0;
        return inboxMapper.deleteBatchByIds(username, ids);
    }

    /**
     * 单条物理删除：仅本人记录会被命中。
     */
    @Transactional
    public boolean removeOne(String username, Long id)
    {
        if (username == null || id == null) return false;
        return inboxMapper.deleteByIdAndUser(id, username) > 0;
    }

    /**
     * 简化的分页结果对象——避免 service 直接依赖 web 层 TableDataInfo。
     * controller 拿到后再包装成项目内的 TableDataInfo。
     */
    public static class PageResult<T>
    {
        private final List<T> rows;
        private final long total;

        public PageResult(List<T> rows, long total)
        {
            this.rows = rows;
            this.total = total;
        }

        public List<T> getRows() { return rows; }
        public long getTotal() { return total; }
    }

    @Transactional
    public int cleanupExpired(int retainDays)
    {
        int marked = inboxMapper.expireBeforeNow();
        int removed = inboxMapper.deleteExpired(Math.max(1, retainDays));
        if (marked + removed > 0)
        {
            log.info("inbox 清理：标记过期 {}，物理删除 {}", marked, removed);
        }
        return marked + removed;
    }
}
