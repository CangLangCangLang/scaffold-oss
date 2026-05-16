package com.scaffold.framework.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import com.scaffold.common.module.ModuleRegistry;
import com.scaffold.common.module.ScaffoldModule;

/**
 * Q-3 ScaffoldModulesHealthIndicator 单测：
 * <ul>
 *   <li>有模块 → status=UP，details.count + modules 字段齐全</li>
 *   <li>没模块 → status=UP，count=0，modules=空</li>
 *   <li>summarize 输出 name / version / enabled 三个字段</li>
 * </ul>
 */
class ScaffoldModulesHealthIndicatorTest
{
    @Test
    void healthUpWithRegisteredModules()
    {
        ModuleRegistry registry = mock(ModuleRegistry.class);
        when(registry.all()).thenReturn(List.of(
                ScaffoldModule.of("crm", "1.0.0", "CRM 客户关系管理"),
                ScaffoldModule.of("file", "1.0.0", "文件中心")
        ));
        ScaffoldModulesHealthIndicator indicator = new ScaffoldModulesHealthIndicator(registry);

        Health h = indicator.health();

        assertThat(h.getStatus()).isEqualTo(Status.UP);
        assertThat(h.getDetails()).containsEntry("count", 2);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> modules = (List<Map<String, Object>>) h.getDetails().get("modules");
        assertThat(modules).hasSize(2);
        assertThat(modules.get(0)).containsKeys("name", "version", "enabled");
        assertThat(modules.get(0).get("name")).isEqualTo("crm");
    }

    @Test
    void healthUpWithEmptyModules()
    {
        ModuleRegistry registry = mock(ModuleRegistry.class);
        when(registry.all()).thenReturn(List.of());
        ScaffoldModulesHealthIndicator indicator = new ScaffoldModulesHealthIndicator(registry);

        Health h = indicator.health();

        assertThat(h.getStatus()).isEqualTo(Status.UP);
        assertThat(h.getDetails()).containsEntry("count", 0);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> modules = (List<Map<String, Object>>) h.getDetails().get("modules");
        assertThat(modules).isEmpty();
    }
}
