package com.scaffold.framework.web.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.scaffold.common.core.storage.FileStorageService;

/**
 * 上传 wrapper 装配：默认提供 {@link DefaultUploadStorageService}，
 * 业务侧若需要自定义（例如接 antivirus 扫描后再落盘），
 * 注入自己的 {@code UploadStorageService} Bean 即可覆盖。
 *
 * @author scaffold
 */
@Configuration
@EnableConfigurationProperties(UploadStorageProperties.class)
public class UploadStorageConfig
{
    @Bean
    @ConditionalOnMissingBean(UploadStorageService.class)
    public UploadStorageService uploadStorageService(FileStorageService fileStorageService,
                                                     UploadStorageProperties properties)
    {
        return new DefaultUploadStorageService(fileStorageService, properties);
    }
}
