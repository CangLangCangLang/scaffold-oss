package com.scaffold.module.report;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import com.scaffold.common.module.ScaffoldModule;

/**
 * 报表中心模块（M-8）自动装配入口。
 *
 * <h3>职能边界</h3>
 * <ul>
 *   <li>sys_report_template：SQL 模板（仅 SELECT；行数 / 超时 / perm_key 三重配额）</li>
 *   <li>sys_report_run_log：执行历史 + 慢查询 + 失败 / 超时分类</li>
 *   <li>sys_report_dashboard / sys_report_dashboard_card：组合看板 + 卡片</li>
 *   <li>sys_report_datasource：外部 JDBC 数据源（密码 AES 可逆加密）</li>
 * </ul>
 *
 * <h3>安全护栏</h3>
 * <ol>
 *   <li>SqlGuard 强制 select-only：拒 DDL/DML/多语句/注释走私 / DROP /  / 函数白名单外注入</li>
 *   <li>ReportRunner 强制行数（默认 10000） + 超时（默认 30s）双闸门</li>
 *   <li>每模板独立 perm_key + 全局 report:template:run 双校验</li>
 *   <li>外部数据源密码 Aes256Util 加密落库，仅运行时由 service 解密装载到 HikariDataSource</li>
 * </ol>
 *
 * <h3>关停</h3>
 * 临时关：{@code app.module.report.enabled=false}；
 * 永久卸载：admin/pom.xml 删本依赖 + 跑 {@code report_uninstall.sql}（应用过的 changeset 不会自动回滚）。
 *
 * <h3>外部依赖</h3>
 * 仅依赖 framework 的 {@code Aes256Util} / {@code BaseController}；不依赖任何业务模块。
 *
 * @author scaffold
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "app.module.report", name = "enabled",
        havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.scaffold.module.report")
@MapperScan(basePackages = "com.scaffold.module.report.mapper")
public class ReportModuleAutoConfiguration
{
    private static final String MODULE_NAME = "report";
    private static final String MODULE_VERSION = "3.9.2";
    private static final String MODULE_DESC = "报表中心（SQL 模板 + 行数/超时/权限三闸 + 看板 + 外部数据源）";

    @Bean
    public ScaffoldModule reportModuleDescriptor()
    {
        return ScaffoldModule.of(MODULE_NAME, MODULE_VERSION, MODULE_DESC);
    }
}
