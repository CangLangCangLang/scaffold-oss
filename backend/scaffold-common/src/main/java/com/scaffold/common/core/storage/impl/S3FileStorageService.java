package com.scaffold.common.core.storage.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import com.scaffold.common.core.storage.FileStorageService;
import com.scaffold.common.core.storage.properties.FileStorageProperties;
import com.scaffold.common.utils.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * 基于 AWS S3 SDK v2 的对象存储实现，兼容 MinIO / 阿里云 OSS / 腾讯云 COS 等 S3 兼容服务。
 * <p>
 * 装配条件：{@code file.storage.type=s3}，参考 {@link com.scaffold.framework.config.FileStorageConfig}。
 *
 * @author scaffold
 */
public class S3FileStorageService implements FileStorageService
{
    private final FileStorageProperties properties;
    private final S3Client s3Client;

    public S3FileStorageService(FileStorageProperties properties)
    {
        this.properties = properties;
        FileStorageProperties.S3 cfg = properties.getS3();
        AwsBasicCredentials credentials = AwsBasicCredentials.create(cfg.getAccessKey(), cfg.getSecretKey());
        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(cfg.getEndpoint()))
                .region(Region.of(cfg.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(cfg.isPathStyle())
                        .build())
                .build();
    }

    @Override
    public String store(String objectKey, InputStream input, String contentType, long size) throws IOException
    {
        FileStorageProperties.S3 cfg = properties.getS3();
        PutObjectRequest.Builder req = PutObjectRequest.builder()
                .bucket(cfg.getBucket())
                .key(stripLeadingSlash(objectKey));
        if (StringUtils.isNotEmpty(contentType))
        {
            req.contentType(contentType);
        }
        s3Client.putObject(req.build(), RequestBody.fromInputStream(input, size));
        return resolveUrl(objectKey);
    }

    @Override
    public String resolveUrl(String objectKey)
    {
        FileStorageProperties.S3 cfg = properties.getS3();
        String key = stripLeadingSlash(objectKey);
        if (StringUtils.isNotEmpty(cfg.getPublicUrl()))
        {
            return rstrip(cfg.getPublicUrl()) + "/" + key;
        }
        if (cfg.isPathStyle())
        {
            return rstrip(cfg.getEndpoint()) + "/" + cfg.getBucket() + "/" + key;
        }
        URI base = URI.create(rstrip(cfg.getEndpoint()));
        return base.getScheme() + "://" + cfg.getBucket() + "." + base.getHost()
                + (base.getPort() > 0 ? ":" + base.getPort() : "")
                + "/" + key;
    }

    @Override
    public boolean delete(String objectKey)
    {
        FileStorageProperties.S3 cfg = properties.getS3();
        try
        {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(cfg.getBucket())
                    .key(stripLeadingSlash(objectKey))
                    .build());
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    @Override
    public String type()
    {
        return "s3";
    }

    private String stripLeadingSlash(String key)
    {
        if (key == null) return "";
        return key.startsWith("/") ? key.substring(1) : key;
    }

    private String rstrip(String s)
    {
        if (s == null || s.isEmpty()) return s;
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
