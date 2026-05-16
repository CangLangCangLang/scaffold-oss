package com.scaffold.module.cms.inbox.bridge;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import com.scaffold.common.module.ScaffoldModule;
import com.scaffold.framework.web.websocket.bus.MessagePublisher;
import com.scaffold.module.cms.event.ArticleStatusChangedEvent;

/**
 * CMS × Inbox 通知桥的自动装配（M-5）。<br>
 * 装配条件：
 * <ul>
 *   <li>{@link ArticleStatusChangedEvent}（cms 模块） + {@link MessagePublisher}（framework）
 *       同时存在 —— cms 或 framework push bus 不可用时桥模块整体跳过</li>
 *   <li>{@code app.module.cms.inbox.enabled=true}（默认开）</li>
 * </ul>
 *
 * <p>这个桥模块**不依赖** scaffold-module-inbox：MessagePublisher 在 framework 自带的总线里，
 * inbox 模块只是 push bus 的一个 recorder。框架已注册 inbox recorder 时自动落 message_inbox 表，
 * 否则仅做 WebSocket 实时推送（仍然能在浏览器右上角铃铛叫一下）。</p>
 */
@AutoConfiguration
@ConditionalOnClass({ArticleStatusChangedEvent.class, MessagePublisher.class})
@ConditionalOnProperty(prefix = "app.module.cms.inbox", name = "enabled",
        havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.scaffold.module.cms.inbox.bridge")
public class CmsInboxBridgeAutoConfiguration
{
    private static final String MODULE_NAME = "cms-inbox";
    private static final String MODULE_VERSION = "3.9.2";
    private static final String MODULE_DESC = "CMS × Inbox 通知桥（M-5）：文章状态变更 → 给作者发站内信";

    @Bean
    public ScaffoldModule cmsInboxBridgeModuleDescriptor()
    {
        return ScaffoldModule.of(MODULE_NAME, MODULE_VERSION, MODULE_DESC);
    }
}
