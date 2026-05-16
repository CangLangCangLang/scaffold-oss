package com.scaffold.module.workflow;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import com.scaffold.common.module.ScaffoldModule;

/**
 * 工作流模块自动装配入口（Flowable 8 / BPMN 2.0）。
 * <p>
 * 通过 {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports} 注册，
 * 引入本模块 jar 即自动启用；临时关停：
 * <pre>app.module.workflow.enabled=false</pre>
 *
 * <h3>建表方式</h3>
 * Flowable 引擎自带的 {@code ACT_*} 表由 {@code flowable-spring-boot-starter-process} 启动时
 * 通过自身 schema 升级机制建好，同 DataSource 即可（不进入主项目 Liquibase 流水）。
 * 默认行为可由 {@code flowable.database-schema-update=true|false|create-drop} 控制。
 *
 * @author scaffold
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "app.module.workflow", name = "enabled",
        havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.scaffold.module.workflow")
public class WorkflowModuleAutoConfiguration
{
    private static final String MODULE_NAME = "workflow";
    private static final String MODULE_VERSION = "3.9.2";
    private static final String MODULE_DESC = "工作流（Flowable 8 / BPMN 2.0），含部署、启动、待办、完成";

    @Bean
    public ScaffoldModule workflowModuleDescriptor()
    {
        return ScaffoldModule.of(MODULE_NAME, MODULE_VERSION, MODULE_DESC);
    }
}
