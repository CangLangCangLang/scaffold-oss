package com.scaffold.module.file.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.scaffold.module.file.service.FileService;

/**
 * Quartz 定时清理：每天凌晨 3 点跑一次，把软删超 30 天 + ref_count=0 的文件物理清磁盘 + 清 DB。
 *
 * <p>{@code sys_job} 行已在 {@code file_menu.sql} 预装：
 * <pre>
 *   invoke_target = "fileCleanupJob.purge()"
 *   cron_expression = "0 0 3 * * ?"
 * </pre>
 *
 * <p>{@code @Component("fileCleanupJob")} 别名很关键 — scaffold-quartz 通过 SpEL 反射 bean 名 + 方法名调度。
 * 没装 quartz 时本 bean 也无害（只是不会被自动触发，可手工调 {@code POST /file/file/purge-now}）。
 *
 * @author scaffold
 */
@Component("fileCleanupJob")
public class FileCleanupJob
{
    private static final Logger log = LoggerFactory.getLogger(FileCleanupJob.class);

    @Autowired private FileService fileService;

    /** Quartz 默认入口；也可被管理员手工触发的 controller 调用。 */
    public void purge()
    {
        try
        {
            int n = fileService.purgeExpired(FileService.DEFAULT_RETAIN_DAYS);
            log.info("[FileCleanupJob] purgeExpired finished, removed {} files", n);
        }
        catch (Exception e)
        {
            log.warn("[FileCleanupJob] purgeExpired failed: {}", e.getMessage(), e);
        }
    }
}
