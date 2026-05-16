package com.scaffold.framework.web.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 通用上传存储服务（业务层 wrapper）。
 *
 * <p>定位与 {@link com.scaffold.common.core.storage.FileStorageService} 不同：
 * <ul>
 *   <li>{@code FileStorageService} 是 <b>底层</b> 抽象（local / s3 切换），输入是 InputStream + objectKey</li>
 *   <li>{@code UploadStorageService} 是 <b>上层</b> 业务 wrapper，输入是 MultipartFile + 业务 bucket，
 *       负责扩展名白名单 / 大小限制 / 路径生成等"上传场景"安全控制；落盘委托给 {@link com.scaffold.common.core.storage.FileStorageService}</li>
 * </ul>
 *
 * <h3>路径与 URL 约定</h3>
 * <p>{@code bucket} 支持带命名空间的多级路径（如 "cms/image"、"form/file"），最终：
 * <ul>
 *   <li>对象 key：{@code <bucket>/yyyyMM/<uuid>.<ext>}</li>
 *   <li>对外 URL：{@code /profile/<bucket>/yyyyMM/<uuid>.<ext>}（local 实现走 ResourcesConfig 的 /profile/** 静态映射）</li>
 * </ul>
 *
 * <h3>典型调用</h3>
 * <pre>
 *   String url = uploadStorageService.save(file, "cms/image");          // CMS 富文本图片
 *   String url = uploadStorageService.save(file, "form/file", opts);    // 表单引擎附件，自定义白名单 / 上限
 * </pre>
 *
 * @author scaffold
 */
public interface UploadStorageService
{
    /**
     * 用默认上传策略（默认白名单 + 默认大小上限）保存文件。
     *
     * @param file 上传内容；为空 / null 抛 ServiceException
     * @param bucket 业务桶名，支持斜杠分隔多级（如 "cms/image"、"form/file"）；不允许 ".." 或反斜杠
     * @return 对外可访问的 URL，例如 {@code /profile/cms/image/202605/abc.jpg}
     */
    String save(MultipartFile file, String bucket);

    /**
     * 自定义上传策略保存文件，覆盖默认白名单 / 上限。
     *
     * @param file 上传内容
     * @param bucket 业务桶名
     * @param options 上传选项，覆盖全局默认
     * @return 对外可访问的 URL
     */
    String save(MultipartFile file, String bucket, UploadOptions options);
}
