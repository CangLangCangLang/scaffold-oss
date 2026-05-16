package com.scaffold.module.file;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import com.scaffold.common.module.ScaffoldModule;

/**
 * 文件中心模块（M-6）自动装配入口。
 *
 * <h3>职能边界</h3>
 * <ul>
 *   <li>sys_file：上传记录主表（bucket / 物理 path / mime / 大小 / 软删 / 引用计数）</li>
 *   <li>sys_file_folder：用户级文件夹树（owner_id 隔离，软删，唯一 path）</li>
 *   <li>sys_file_share：分享链接（带过期 / 一次性 token / 访问次数累加）</li>
 *   <li>sys_file_ref：跨模块引用计数（CMS 文章 / Form 提交 引用文件后引用 +1，删除前必须为 0）</li>
 * </ul>
 *
 * <h3>关停</h3>
 * 临时关：{@code app.module.file.enabled=false}（默认开启）；
 * 永久卸载：admin/pom.xml 删本依赖 + 跑 {@code file_uninstall.sql}（已应用 changeset 不会自动回滚）。
 *
 * <h3>外部依赖</h3>
 * 仅依赖 framework 的 {@code UploadStorageService}（落 {@code /profile/file/yyyyMM/uuid.ext}）；
 * 卸载本模块对 CMS / Form 等其它模块零影响（它们只用到 framework 的通用上传，没耦合本模块）。
 *
 * @author scaffold
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "app.module.file", name = "enabled",
        havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.scaffold.module.file")
@MapperScan(basePackages = "com.scaffold.module.file.mapper")
public class FileModuleAutoConfiguration
{
    private static final String MODULE_NAME = "file";
    private static final String MODULE_VERSION = "3.9.2";
    private static final String MODULE_DESC = "文件中心（上传记录 + 文件夹 + 分享链接 + 引用计数 + 两阶段软删）";

    @Bean
    public ScaffoldModule fileModuleDescriptor()
    {
        return ScaffoldModule.of(MODULE_NAME, MODULE_VERSION, MODULE_DESC);
    }
}
