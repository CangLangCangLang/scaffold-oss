package com.scaffold.framework.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.scaffold.common.core.storage.FileStorageService;
import com.scaffold.common.core.storage.impl.LocalFileStorageService;
import com.scaffold.common.core.storage.impl.S3FileStorageService;
import com.scaffold.common.core.storage.properties.FileStorageProperties;

/**
 * 文件存储装配。
 * <p>
 * 通过 {@code file.storage.type} 切换实现：
 * <ul>
 *   <li>{@code local}（默认）：磁盘存储</li>
 *   <li>{@code s3}：S3 兼容存储（AWS / MinIO / OSS / COS 等）</li>
 * </ul>
 * 业务侧实现 {@link FileStorageService} 并暴露为 Bean 时，将覆盖内置实现。
 *
 * @author scaffold
 */
@Configuration
@EnableConfigurationProperties(FileStorageProperties.class)
public class FileStorageConfig
{
    @Bean
    @ConditionalOnMissingBean(FileStorageService.class)
    @ConditionalOnProperty(name = "file.storage.type", havingValue = "s3")
    public FileStorageService s3FileStorageService(FileStorageProperties properties)
    {
        validateS3(properties);
        return new S3FileStorageService(properties);
    }

    @Bean
    @ConditionalOnMissingBean(FileStorageService.class)
    public FileStorageService localFileStorageService(FileStorageProperties properties)
    {
        return new LocalFileStorageService(properties);
    }

    private void validateS3(FileStorageProperties properties)
    {
        FileStorageProperties.S3 cfg = properties.getS3();
        if (cfg == null
                || isBlank(cfg.getEndpoint())
                || isBlank(cfg.getAccessKey())
                || isBlank(cfg.getSecretKey())
                || isBlank(cfg.getBucket()))
        {
            throw new IllegalStateException(
                    "file.storage.type=s3 但缺少必要参数：endpoint / accessKey / secretKey / bucket，"
                    + "请通过 application.yml 或环境变量补齐。");
        }
    }

    private boolean isBlank(String s)
    {
        return s == null || s.trim().isEmpty();
    }
}
