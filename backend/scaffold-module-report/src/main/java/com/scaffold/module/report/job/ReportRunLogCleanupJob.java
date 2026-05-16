package com.scaffold.module.report.job;

import java.util.Calendar;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.scaffold.module.report.service.RunService;

/**
 * 报表运行日志清理（Quartz 调用入口）。
 *
 * <p>由 {@code sys_job} 表里的 7025 条目（{@code reportRunLogCleanupJob.purge()}）每天凌晨 4 点跑。
 * 默认保留 90 天，可通过配置 {@code app.module.report.runlog-keep-days} 调整。</p>
 *
 * @author scaffold
 */
@Component("reportRunLogCleanupJob")
public class ReportRunLogCleanupJob
{
    private static final Logger log = LoggerFactory.getLogger(ReportRunLogCleanupJob.class);

    @Autowired
    private RunService runService;

    @Value("${app.module.report.runlog-keep-days:90}")
    private int keepDays;

    public void purge()
    {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, -keepDays);
        Date threshold = c.getTime();
        int n = runService.purgeOlderThan(threshold);
        log.info("[report-runlog-cleanup] threshold={} purged={}", threshold, n);
    }
}
