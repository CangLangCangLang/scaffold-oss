package com.scaffold.common.core.storage.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import com.scaffold.common.config.ScaffoldConfig;
import com.scaffold.common.core.storage.FileStorageService;
import com.scaffold.common.core.storage.properties.FileStorageProperties;
import com.scaffold.common.utils.StringUtils;

/**
 * 本地磁盘存储实现。沿用 {@link ScaffoldConfig#getProfile()} 的根目录（也可由
 * {@code file.storage.local-root} 显式覆盖），与 {@code addResourceHandlers} 暴露的
 * {@code /profile/**} 路径协同。
 *
 * @author scaffold
 */
public class LocalFileStorageService implements FileStorageService
{
    private final FileStorageProperties properties;

    public LocalFileStorageService(FileStorageProperties properties)
    {
        this.properties = properties;
    }

    @Override
    public String store(String objectKey, InputStream input, String contentType, long size) throws IOException
    {
        Path target = resolvePath(objectKey);
        Files.createDirectories(target.getParent());
        Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        return resolveUrl(objectKey);
    }

    @Override
    public String resolveUrl(String objectKey)
    {
        String prefix = StringUtils.isEmpty(properties.getUrlPrefix()) ? "/profile" : properties.getUrlPrefix();
        if (!prefix.startsWith("/"))
        {
            prefix = "/" + prefix;
        }
        if (!objectKey.startsWith("/"))
        {
            return prefix + "/" + objectKey;
        }
        return prefix + objectKey;
    }

    @Override
    public boolean delete(String objectKey)
    {
        try
        {
            return Files.deleteIfExists(resolvePath(objectKey));
        }
        catch (IOException e)
        {
            return false;
        }
    }

    @Override
    public String type()
    {
        return "local";
    }

    private Path resolvePath(String objectKey)
    {
        String root = StringUtils.isEmpty(properties.getLocalRoot()) ? ScaffoldConfig.getProfile() : properties.getLocalRoot();
        return Paths.get(root, objectKey).toAbsolutePath().normalize();
    }
}
