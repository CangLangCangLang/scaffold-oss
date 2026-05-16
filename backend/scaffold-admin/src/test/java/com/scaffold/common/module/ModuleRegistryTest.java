package com.scaffold.common.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;

class ModuleRegistryTest
{
    @SuppressWarnings("unchecked")
    @Test
    void allReturnsImmutableSnapshotOfRegisteredModules()
    {
        ScaffoldModule a = ScaffoldModule.of("inbox", "1.0", "离线消息盒");
        ScaffoldModule b = ScaffoldModule.of("workflow", "0.1", "工作流");
        ObjectProvider<ScaffoldModule> provider = mock(ObjectProvider.class);
        when(provider.orderedStream()).thenReturn(Stream.of(a, b));

        ModuleRegistry registry = new ModuleRegistry(provider);
        List<ScaffoldModule> all = registry.all();
        assertThat(all).extracting(ScaffoldModule::getName).containsExactly("inbox", "workflow");
        // 返回的是不可变视图
        assertThat(all).isUnmodifiable();
    }

    @SuppressWarnings("unchecked")
    @Test
    void byNameLooksUpModuleByExactName()
    {
        ObjectProvider<ScaffoldModule> provider = mock(ObjectProvider.class);
        when(provider.orderedStream())
                .thenReturn(Stream.of(ScaffoldModule.of("inbox", "1.0", "x")))
                .thenReturn(Stream.of(ScaffoldModule.of("inbox", "1.0", "x"))); // 第二次调用 byName 时 stream 重新生成
        ModuleRegistry registry = new ModuleRegistry(provider);
        assertThat(registry.byName("inbox")).isPresent();
    }

    @SuppressWarnings("unchecked")
    @Test
    void onApplicationReadyDoesNotThrowOnEmptyRegistry()
    {
        ObjectProvider<ScaffoldModule> provider = mock(ObjectProvider.class);
        when(provider.orderedStream()).thenReturn(Stream.empty());
        ModuleRegistry registry = new ModuleRegistry(provider);
        registry.onApplicationEvent(mock(ApplicationReadyEvent.class));
    }
}
