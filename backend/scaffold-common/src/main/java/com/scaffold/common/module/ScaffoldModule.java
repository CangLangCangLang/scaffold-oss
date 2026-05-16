package com.scaffold.common.module;

import java.util.Objects;

/**
 * 业务模块在容器里的"身份证"。
 * <p>
 * 每个可插拔业务模块在自己的 AutoConfiguration 里发布**唯一一个** {@link ScaffoldModule} Bean，
 * 用于：
 * <ul>
 *   <li>{@link ModuleRegistry} 聚合后由 {@code /actuator/scaffold-modules} 暴露给运维</li>
 *   <li>启动日志一致打印模块加载情况</li>
 *   <li>未来跨模块互相发现（按 name 查询）</li>
 * </ul>
 * 不要把它当 spring bean 注入到业务代码里——它只是元数据。
 *
 * @author scaffold
 */
public final class ScaffoldModule
{
    /** 模块短名（必填，小写中划线，例如 {@code inbox}、{@code workflow}） */
    private final String name;
    /** 模块版本（建议跟随 pom.xml） */
    private final String version;
    /** 简短描述，给 actuator 端点展示 */
    private final String description;
    /** 是否启用（true 时才会被 ModuleRegistry 收录） */
    private final boolean enabled;

    private ScaffoldModule(String name, String version, String description, boolean enabled)
    {
        this.name = Objects.requireNonNull(name, "module name");
        this.version = version == null ? "" : version;
        this.description = description == null ? "" : description;
        this.enabled = enabled;
    }

    public static ScaffoldModule of(String name, String version, String description)
    {
        return new ScaffoldModule(name, version, description, true);
    }

    public String getName() { return name; }
    public String getVersion() { return version; }
    public String getDescription() { return description; }
    public boolean isEnabled() { return enabled; }

    @Override
    public String toString()
    {
        return "ScaffoldModule{" + name + (version.isEmpty() ? "" : "@" + version) + "}";
    }
}
