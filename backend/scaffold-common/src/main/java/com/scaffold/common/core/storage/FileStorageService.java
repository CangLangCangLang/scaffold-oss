package com.scaffold.common.core.storage;

import java.io.IOException;
import java.io.InputStream;

/**
 * 文件存储抽象。
 * <p>
 * 通过 {@code file.storage.type} 切换不同实现（local / s3 / oss 等），
 * 业务侧通过统一接口操作文件，避免与具体存储介质耦合。
 *
 * @author scaffold
 */
public interface FileStorageService
{
    /**
     * 存储文件。
     *
     * @param objectKey   对象键，例如 {@code 2026/03/20/abc.png}
     * @param input       文件输入流，调用方负责关闭原始流
     * @param contentType 媒体类型，可空
     * @param size        文件大小（字节），未知时传 -1
     * @return 客户端可访问的相对 URL，例如 {@code /profile/2026/03/20/abc.png}
     */
    String store(String objectKey, InputStream input, String contentType, long size) throws IOException;

    /**
     * 返回客户端可访问的资源 URL。本地存储返回相对路径，对象存储建议返回预签名 URL。
     */
    String resolveUrl(String objectKey);

    /**
     * 删除文件。
     *
     * @return true 表示删除成功，false 表示对象不存在或删除失败
     */
    boolean delete(String objectKey);

    /**
     * 当前存储实现的类型标识，便于排查环境差异。
     */
    String type();
}
