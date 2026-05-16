package com.scaffold.module.inbox;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.scaffold.module.inbox.service.MessageInboxService;

/**
 * inbox 过期清理任务：默认每天 03:30 跑一次，
 * 可通过 {@code inbox.cleanup.cron} 重写、{@code inbox.cleanup.enabled=false} 禁用。
 *
 * @author scaffold
 */
@Component
@EnableScheduling
@ConditionalOnProperty(name = "inbox.cleanup.enabled", havingValue = "true", matchIfMissing = true)
public class MessageInboxCleanupJob
{
    private final MessageInboxService inboxService;

    @Value("${inbox.cleanup.retain-days:30}")
    private int retainDays;

    public MessageInboxCleanupJob(MessageInboxService inboxService)
    {
        this.inboxService = inboxService;
    }

    @Scheduled(cron = "${inbox.cleanup.cron:0 30 3 * * ?}")
    public void run()
    {
        inboxService.cleanupExpired(retainDays);
    }
}
