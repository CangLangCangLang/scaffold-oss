package com.scaffold.web.actuator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.context.annotation.Configuration;
import com.scaffold.common.module.ModuleRegistry;
import com.scaffold.common.module.ScaffoldModule;

/**
 * {@code GET /actuator/scaffold-modules}：列出当前启用的业务模块。
 * <p>
 * 配置 {@code management.endpoints.web.exposure.include} 时记得包含 {@code scaffold-modules}。
 *
 * @author scaffold
 */
@Configuration
@Endpoint(id = "scaffold-modules")
public class ScaffoldModuleEndpoint
{
    private final ModuleRegistry registry;

    public ScaffoldModuleEndpoint(ModuleRegistry registry)
    {
        this.registry = registry;
    }

    @ReadOperation
    public Map<String, Object> modules()
    {
        List<ScaffoldModule> all = registry.all();
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("count", all.size());
        root.put("modules", all.stream().map(this::serialize).toList());
        return root;
    }

    private Map<String, Object> serialize(ScaffoldModule module)
    {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", module.getName());
        item.put("version", module.getVersion());
        item.put("description", module.getDescription());
        item.put("enabled", module.isEnabled());
        return item;
    }
}
