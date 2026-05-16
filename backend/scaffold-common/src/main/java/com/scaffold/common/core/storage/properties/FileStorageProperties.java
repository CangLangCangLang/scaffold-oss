package com.scaffold.common.core.storage.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 文件存储配置项。
 *
 * @author scaffold
 */
@ConfigurationProperties(prefix = "file.storage")
public class FileStorageProperties
{
    /** 存储类型：local / s3 */
    private String type = "local";

    /** 本地文件根目录，未配置时回退到 ScaffoldConfig.profile */
    private String localRoot;

    /** 资源访问前缀，默认 /profile */
    private String urlPrefix = "/profile";

    /** S3 / MinIO 配置 */
    private S3 s3 = new S3();

    public S3 getS3()
    {
        return s3;
    }

    public void setS3(S3 s3)
    {
        this.s3 = s3;
    }

    public static class S3
    {
        /** S3 服务端点，例如 https://s3.amazonaws.com 或 http://minio:9000 */
        private String endpoint;
        /** 区域，例如 us-east-1 */
        private String region = "us-east-1";
        /** Access key */
        private String accessKey;
        /** Secret key */
        private String secretKey;
        /** Bucket */
        private String bucket;
        /** 是否使用 path-style（MinIO 通常需要 true） */
        private boolean pathStyle = true;
        /** 公开访问 URL 前缀（CDN/反代），未设置时拼接 endpoint+bucket */
        private String publicUrl;

        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        public String getAccessKey() { return accessKey; }
        public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
        public String getBucket() { return bucket; }
        public void setBucket(String bucket) { this.bucket = bucket; }
        public boolean isPathStyle() { return pathStyle; }
        public void setPathStyle(boolean pathStyle) { this.pathStyle = pathStyle; }
        public String getPublicUrl() { return publicUrl; }
        public void setPublicUrl(String publicUrl) { this.publicUrl = publicUrl; }
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getLocalRoot()
    {
        return localRoot;
    }

    public void setLocalRoot(String localRoot)
    {
        this.localRoot = localRoot;
    }

    public String getUrlPrefix()
    {
        return urlPrefix;
    }

    public void setUrlPrefix(String urlPrefix)
    {
        this.urlPrefix = urlPrefix;
    }
}
