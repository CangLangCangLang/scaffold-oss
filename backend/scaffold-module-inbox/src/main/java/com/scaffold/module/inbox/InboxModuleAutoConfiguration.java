package com.scaffold.module.inbox;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import com.scaffold.common.module.ScaffoldModule;

/**
 * 离线消息盒模块自动装配入口。
 * <p>
 * 通过 {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports} 注册，
 * 引入本模块的 jar 即自动启用；要在不删 jar 的情况下临时关掉，配置：
 * <pre>app.module.inbox.enabled=false</pre>
 *
 * @author scaffold
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "app.module.inbox", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.scaffold.module.inbox")
public class InboxModuleAutoConfiguration
{
    private static final String MODULE_NAME = "inbox";
    private static final String MODULE_VERSION = "3.9.2";
    private static final String MODULE_DESC = "离线消息盒（与 WebSocket 推送总线协同的可靠投递）";

    /**
     * 发布一个 {@link ScaffoldModule} Bean，给 {@code /actuator/scaffold-modules} 与启动日志使用。
     */
    @Bean
    public ScaffoldModule inboxModuleDescriptor()
    {
        return ScaffoldModule.of(MODULE_NAME, MODULE_VERSION, MODULE_DESC);
    }
}
