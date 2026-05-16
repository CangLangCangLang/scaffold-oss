package com.scaffold.module.cms.workflow.bridge;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import com.scaffold.common.module.ScaffoldModule;
import com.scaffold.module.cms.service.ArticleService;
import com.scaffold.module.cms.workflow.CmsWorkflowAdapter;
import com.scaffold.module.workflow.service.WorkflowFacade;

/**
 * CMS × Workflow 联动桥的自动装配（M-4）。<br>
 * 装配条件：
 * <ul>
 *   <li>{@link WorkflowFacade} 与 {@link ArticleService} 同时在 classpath 中
 *       —— 没有 cms 或 workflow 模块时桥模块整体跳过，不会让启动崩溃</li>
 *   <li>{@code app.module.cms.workflow.enabled=true} —— 默认关闭；
 *       未显式启用时 CMS 走自闭环状态机，与 M-3 行为一致</li>
 * </ul>
 *
 * <p>桥模块本身不动 CMS 与 workflow 的源码：通过实现 {@code CmsWorkflowAdapter}
 * 让 cms 的 submit 走 startProcess，通过 {@link ArticleWorkflowEventListener} 让
 * Flowable 的 PROCESS_COMPLETED 反向回调 ArticleService 切状态。</p>
 */
@AutoConfiguration
@ConditionalOnClass({WorkflowFacade.class, ArticleService.class})
@ConditionalOnProperty(prefix = "app.module.cms.workflow", name = "enabled",
        havingValue = "true", matchIfMissing = false)
@ComponentScan(basePackages = "com.scaffold.module.cms.workflow.bridge")
public class CmsWorkflowBridgeAutoConfiguration
{
    private static final String MODULE_NAME = "cms-workflow";
    private static final String MODULE_VERSION = "3.9.2";
    private static final String MODULE_DESC = "CMS × Workflow 联动桥（M-4）：CMS 提交审核走 Flowable 真审批流";

    @Bean
    public ScaffoldModule cmsWorkflowBridgeModuleDescriptor()
    {
        return ScaffoldModule.of(MODULE_NAME, MODULE_VERSION, MODULE_DESC);
    }

    @Bean
    public CmsWorkflowAdapter workflowAwareCmsAdapter()
    {
        return new WorkflowAwareCmsAdapter();
    }
}
