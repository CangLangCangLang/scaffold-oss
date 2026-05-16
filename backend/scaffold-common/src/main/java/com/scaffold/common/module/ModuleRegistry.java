package com.scaffold.common.module;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * 全局模块注册表：聚合所有发布了 {@link ScaffoldModule} Bean 的业务模块。
 * <ul>
 *   <li>启动完成后打印一次加载摘要，便于运维确认</li>
 *   <li>{@link com.scaffold.common.module.ScaffoldModuleEndpoint} 通过它对外暴露 actuator 端点</li>
 * </ul>
 *
 * @author scaffold
 */
@Component
public class ModuleRegistry implements ApplicationListener<ApplicationReadyEvent>
{
    private static final Logger log = LoggerFactory.getLogger(ModuleRegistry.class);

    private final ObjectProvider<ScaffoldModule> modules;

    public ModuleRegistry(ObjectProvider<ScaffoldModule> modules)
    {
        this.modules = modules;
    }

    public List<ScaffoldModule> all()
    {
        List<ScaffoldModule> list = modules.orderedStream().toList();
        return Collections.unmodifiableList(list);
    }

    public Optional<ScaffoldModule> byName(String name)
    {
        Objects.requireNonNull(name, "name");
        return all().stream().filter(m -> m.getName().equals(name)).findFirst();
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event)
    {
        List<ScaffoldModule> list = all();
        if (list.isEmpty())
        {
            log.info("Scaffold modules: <none enabled>");
            return;
        }
        StringBuilder sb = new StringBuilder("Scaffold modules enabled (").append(list.size()).append("):");
        for (ScaffoldModule module : list)
        {
            sb.append("\n  • ").append(module.getName());
            if (!module.getVersion().isEmpty()) sb.append(" @ ").append(module.getVersion());
            if (!module.getDescription().isEmpty()) sb.append("  — ").append(module.getDescription());
        }
        log.info(sb.toString());
    }
}
