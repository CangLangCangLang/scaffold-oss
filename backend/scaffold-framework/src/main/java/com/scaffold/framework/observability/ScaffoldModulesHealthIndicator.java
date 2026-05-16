package com.scaffold.framework.observability;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import com.scaffold.common.module.ModuleRegistry;
import com.scaffold.common.module.ScaffoldModule;

/**
 * {@code /actuator/health/scaffoldModules} —— 聚合 {@link ModuleRegistry} 的模块清单。
 *
 * <p>所有注册过的模块被算作 UP（说明 Bean 实际加载并且 AutoConfig 通过）；
 * 没注册等于"该模块禁用 / jar 缺失"。
 *
 * <p>在不存在 ModuleRegistry 时（极简 framework-only 项目）直接报 UP 但 modules=空。
 */
@Component("scaffoldModulesHealthIndicator")
public class ScaffoldModulesHealthIndicator implements HealthIndicator
{
    private final ModuleRegistry registry;

    public ScaffoldModulesHealthIndicator(ModuleRegistry registry)
    {
        this.registry = registry;
    }

    @Override
    public Health health()
    {
        List<ScaffoldModule> modules = registry.all();
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("count", modules.size());
        details.put("modules", modules.stream().map(this::summarize).toList());
        return Health.up().withDetails(details).build();
    }

    private Map<String, Object> summarize(ScaffoldModule m)
    {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", m.getName());
        item.put("version", m.getVersion());
        item.put("enabled", m.isEnabled());
        return item;
    }
}
