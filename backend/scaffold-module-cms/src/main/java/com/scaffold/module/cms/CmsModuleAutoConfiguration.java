package com.scaffold.module.cms;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import com.scaffold.common.module.ScaffoldModule;
import com.scaffold.module.cms.workflow.CmsWorkflowAdapter;
import com.scaffold.module.cms.workflow.DefaultCmsWorkflowAdapter;

/**
 * CMS 模块自动装配入口（栏目 / 文章 / 标签 / 状态机 / 公开 API / 富文本图片上传）。
 * <p>
 * 通过 {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports} 注册，
 * 引入本模块 jar 即自动启用；临时关停：
 * <pre>app.module.cms.enabled=false</pre>
 *
 * <h3>建表方式</h3>
 * 4 张业务表（cms_channel / cms_article / cms_tag / cms_article_tag）由模块自有
 * Liquibase changelog（{@code db/changelog/module-cms.yml}）建好，被主 changelog
 * include；删模块 jar 后已应用 changeset 不会回滚，需要清表请执行
 * {@code db/changelog/sql/cms_uninstall.sql}。
 *
 * <h3>工作流接入</h3>
 * 第一批不接入。仅在 service 层预留 {@code CmsWorkflowAdapter} 接口（默认空实现），
 * 将来可通过 {@code @ConditionalOnClass(WorkflowFacade.class)} 注入真正实现，
 * 把 "提交审核" 转成 workflow 启动；启停 cms 模块自身**不依赖** workflow 模块。
 *
 * @author scaffold
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "app.module.cms", name = "enabled",
        havingValue = "true", matchIfMissing = true)
@ComponentScan(
    basePackages = "com.scaffold.module.cms",
    // 桥包默认关：cms-workflow（Article 桥）。主扫描排除，让桥的 AutoConfig
    // 凭 @ConditionalOnProperty(app.module.cms.workflow.enabled=true) 决定加载。
    excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
        type = org.springframework.context.annotation.FilterType.REGEX,
        pattern = "com\\.scaffold\\.module\\.cms\\.(workflow|inbox)\\.bridge\\..*"
    )
)
@MapperScan(basePackages = "com.scaffold.module.cms.mapper")
public class CmsModuleAutoConfiguration
{
    private static final String MODULE_NAME = "cms";
    private static final String MODULE_VERSION = "3.9.2";
    private static final String MODULE_DESC = "CMS 内容管理（栏目 / 文章 / 标签 / 状态机 / 公开 API）";

    @Bean
    public ScaffoldModule cmsModuleDescriptor()
    {
        return ScaffoldModule.of(MODULE_NAME, MODULE_VERSION, MODULE_DESC);
    }

    /**
     * 注册默认 {@link CmsWorkflowAdapter} 空实现。仅当上下文里没有更具体的实现时生效
     * （例如 cms-workflow 桥模块的 {@code WorkflowAwareCmsAdapter}）。
     *
     * <p>之前用 {@code @Component} + {@code @ConditionalOnMissingBean} 在 Spring Boot 4 下
     * 偶发"No qualifying bean of type CmsWorkflowAdapter"，原因是 ComponentScan 评估
     * condition 的时机早于桥模块 bean 注册。改用 {@code @Bean} 形式后由 AutoConfiguration
     * 阶段统一裁决，时机稳定。
     */
    @Bean
    @ConditionalOnMissingBean(CmsWorkflowAdapter.class)
    @ConditionalOnProperty(prefix = "app.module.cms.workflow", name = "enabled",
            havingValue = "false", matchIfMissing = true)
    public CmsWorkflowAdapter defaultCmsWorkflowAdapter()
    {
        return new DefaultCmsWorkflowAdapter();
    }
}
