package com.scaffold.module.form;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import com.scaffold.common.module.ScaffoldModule;

/**
 * 通用表单引擎模块（M-10）自动装配入口。
 *
 * <h3>职能边界</h3>
 * <ul>
 *   <li>表单模板（{@code form_template}）：JSON schema + 版本 + 状态（草稿 / 发布 / 归档）</li>
 *   <li>表单提交（{@code form_submission}）：混合存储 — 平台元数据列（submitter / 状态 / 时间）+ data JSON 列</li>
 *   <li>不参与流程审批；与 Workflow 的 wf_form_schema 互不交叉</li>
 * </ul>
 *
 * <h3>关停</h3>
 * 临时关：{@code app.module.form.enabled=false}（默认开启）；
 * 永久卸载：admin/pom.xml 删本依赖 + 跑 {@code form_uninstall.sql}（已应用 changeset 不会自动回滚）。
 *
 * @author scaffold
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "app.module.form", name = "enabled",
        havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.scaffold.module.form")
@MapperScan(basePackages = "com.scaffold.module.form.mapper")
public class FormModuleAutoConfiguration
{
    private static final String MODULE_NAME = "form";
    private static final String MODULE_VERSION = "3.9.2";
    private static final String MODULE_DESC = "通用表单引擎（模板库 + 渲染器 + 提交记录）";

    @Bean
    public ScaffoldModule formModuleDescriptor()
    {
        return ScaffoldModule.of(MODULE_NAME, MODULE_VERSION, MODULE_DESC);
    }
}
