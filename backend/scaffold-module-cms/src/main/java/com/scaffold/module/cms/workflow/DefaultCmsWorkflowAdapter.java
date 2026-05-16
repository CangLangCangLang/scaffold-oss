package com.scaffold.module.cms.workflow;

/**
 * 默认空实现：始终返回 false，由 CMS 自己走状态机。<br>
 * 通过 {@link com.scaffold.module.cms.CmsModuleAutoConfiguration#defaultCmsWorkflowAdapter()}
 * 以 {@code @Bean + @ConditionalOnMissingBean} 注册（不再用 {@code @Component} 直接扫描，
 * 避免 Spring Boot 4 下 ComponentScan 与 @ConditionalOnMissingBean 的时机竞态导致 default
 * bean 漏装、articleService 报 "No qualifying bean of type CmsWorkflowAdapter"）。
 *
 * <p>当 cms-workflow 桥模块装载（{@code app.module.cms.workflow.enabled=true}）时，
 * 桥里的 {@code WorkflowAwareCmsAdapter} 会作为更具体的 bean 注册，本默认实现不再生效。
 */
public class DefaultCmsWorkflowAdapter implements CmsWorkflowAdapter
{
    @Override
    public boolean onSubmit(Long articleId, String userId)
    {
        return false;
    }
}
