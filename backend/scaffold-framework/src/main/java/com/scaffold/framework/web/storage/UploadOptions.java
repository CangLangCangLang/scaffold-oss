package com.scaffold.framework.web.storage;

import java.util.Set;

/**
 * 上传选项：业务侧覆盖默认白名单 / 大小限制时使用。
 *
 * <p>所有字段为 null 时回落到 {@link UploadStorageProperties} 全局默认；
 * 显式赋值优先级最高（让某个特定 bucket 接受更宽 / 更严的策略）。
 *
 * @author scaffold
 */
public class UploadOptions
{
    /** 允许的扩展名白名单（小写，无前导点）；null = 用全局默认 */
    private Set<String> allowedExtensions;
    /** 最大字节数；null 或 <=0 = 用全局默认 */
    private Long maxBytes;

    public UploadOptions() {}

    public UploadOptions(Set<String> allowedExtensions, Long maxBytes)
    {
        this.allowedExtensions = allowedExtensions;
        this.maxBytes = maxBytes;
    }

    public Set<String> getAllowedExtensions() { return allowedExtensions; }
    public void setAllowedExtensions(Set<String> allowedExtensions) { this.allowedExtensions = allowedExtensions; }
    public Long getMaxBytes() { return maxBytes; }
    public void setMaxBytes(Long maxBytes) { this.maxBytes = maxBytes; }

    /** 便捷构造：仅覆盖扩展名白名单。 */
    public static UploadOptions exts(Set<String> exts)
    {
        UploadOptions o = new UploadOptions();
        o.allowedExtensions = exts;
        return o;
    }

    /** 便捷构造：仅覆盖大小上限（MB）。 */
    public static UploadOptions maxSizeMb(int mb)
    {
        UploadOptions o = new UploadOptions();
        o.maxBytes = (long) mb * 1024L * 1024L;
        return o;
    }
}
