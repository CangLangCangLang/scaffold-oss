package com.scaffold.framework.web.storage;

import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 上传存储默认策略配置。
 *
 * <pre>
 * app:
 *   upload:
 *     max-size-mb: 10
 *     allowed-extensions:
 *       - jpg
 *       - png
 *       - pdf
 * </pre>
 *
 * 业务侧可在调用时通过 {@link UploadOptions} 局部覆盖。
 *
 * @author scaffold
 */
@ConfigurationProperties(prefix = "app.upload")
public class UploadStorageProperties
{
    /** 默认最大上传大小（MB），默认 10 */
    private int maxSizeMb = 10;

    /** 默认扩展名白名单；为空时回落到代码内置（图片 + 常见文档） */
    private Set<String> allowedExtensions;

    public int getMaxSizeMb() { return maxSizeMb; }
    public void setMaxSizeMb(int maxSizeMb) { this.maxSizeMb = maxSizeMb; }
    public Set<String> getAllowedExtensions() { return allowedExtensions; }
    public void setAllowedExtensions(Set<String> allowedExtensions) { this.allowedExtensions = allowedExtensions; }
}
