package com.scaffold.module.report.runtime;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.scaffold.common.exception.ServiceException;
import com.scaffold.module.report.dto.RunResult;
import com.scaffold.module.report.security.ReportParamBinder;
import com.scaffold.module.report.security.ReportSqlGuard;

/**
 * 报表执行器：三闸门组合。
 *
 * <ol>
 *   <li><b>SqlGuard</b>：select-only 校验（{@link ReportSqlGuard}）</li>
 *   <li><b>行数</b>：rowLimit（执行端用 {@code LIMIT} 协议无关，故走 ResultSet 截断 + setMaxRows 双保险）</li>
 *   <li><b>超时</b>：timeoutMs，{@code Statement.setQueryTimeout(s)}（驱动需支持）</li>
 * </ol>
 *
 * <h3>类型映射</h3>
 * <ul>
 *   <li>BigDecimal → Double（前端 ECharts / 表格友好）</li>
 *   <li>Timestamp / Date → ISO 字符串</li>
 *   <li>byte[] → "[binary N bytes]" 占位（不传给前端）</li>
 *   <li>其它原样 toString 后通过 ResultSet.getObject 拉</li>
 * </ul>
 *
 * @author scaffold
 */
@Component
public class ReportRunner
{
    private static final Logger log = LoggerFactory.getLogger(ReportRunner.class);

    private final ReportDataSourceManager dataSourceManager;
    private final int globalRowLimit;
    private final int globalTimeoutMs;
    private final long slowThresholdMs;

    public ReportRunner(ReportDataSourceManager dataSourceManager,
                        @Value("${app.module.report.row-limit:10000}") int globalRowLimit,
                        @Value("${app.module.report.timeout-ms:30000}") int globalTimeoutMs,
                        @Value("${app.module.report.slow-threshold-ms:3000}") long slowThresholdMs)
    {
        this.dataSourceManager = dataSourceManager;
        this.globalRowLimit = globalRowLimit;
        this.globalTimeoutMs = globalTimeoutMs;
        this.slowThresholdMs = slowThresholdMs;
    }

    public int globalRowLimit() { return globalRowLimit; }
    public int globalTimeoutMs() { return globalTimeoutMs; }

    /**
     * 执行查询；该方法只负责执行 + 截断 + 取结果，落运行日志由 service 层包一层 try / finally 处理。
     *
     * @param datasourceId 数据源 id（0 = 主库）
     * @param templateSql 含 {@code ${name}} 占位的模板 SQL
     * @param params 参数键值
     * @param rowLimit 本次行数上限（再被 globalRowLimit 兜一次）
     * @param timeoutMs 本次超时上限（再被 globalTimeoutMs 兜一次）
     */
    public RunResult execute(Long datasourceId,
                             String templateSql,
                             Map<String, Object> params,
                             Integer rowLimit,
                             Integer timeoutMs)
    {
        ReportSqlGuard.ensureSelectOnly(templateSql);

        ReportParamBinder.Result bound = ReportParamBinder.bind(templateSql, params);

        int effRows = clamp(rowLimit, 1, globalRowLimit, globalRowLimit);
        int effTimeout = clamp(timeoutMs, 100, globalTimeoutMs, globalTimeoutMs);

        DataSource ds = dataSourceManager.resolve(datasourceId);
        long start = System.nanoTime();
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(bound.getSql(),
                     ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY))
        {
            try
            {
                conn.setReadOnly(true);
            }
            catch (SQLException ignore)
            {
                // 部分驱动不支持，软失败
            }
            ps.setFetchSize(Math.min(1024, effRows));
            ps.setMaxRows(effRows + 1);
            ps.setQueryTimeout((int) Math.ceil(effTimeout / 1000.0));

            for (int i = 0; i < bound.getValues().size(); i++)
            {
                ps.setObject(i + 1, bound.getValues().get(i));
            }

            try (ResultSet rs = ps.executeQuery())
            {
                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();
                List<String> cols = new ArrayList<>(colCount);
                List<String> types = new ArrayList<>(colCount);
                for (int i = 1; i <= colCount; i++)
                {
                    cols.add(meta.getColumnLabel(i));
                    types.add(meta.getColumnTypeName(i));
                }

                List<List<Object>> rows = new ArrayList<>();
                boolean truncated = false;
                while (rs.next())
                {
                    if (rows.size() >= effRows)
                    {
                        truncated = true;
                        break;
                    }
                    List<Object> row = new ArrayList<>(colCount);
                    for (int i = 1; i <= colCount; i++)
                    {
                        row.add(normalize(rs.getObject(i)));
                    }
                    rows.add(row);
                }

                long costMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
                if (costMs >= slowThresholdMs)
                {
                    log.warn("[report-slow] datasource={} cost={}ms rows={} sql={}",
                            datasourceId, costMs, rows.size(),
                            previewSql(bound.getSql()));
                }
                return RunResult.of(cols, types, rows, costMs, truncated,
                        previewSql(bound.getSql()), bound.getValues());
            }
        }
        catch (SQLTimeoutException e)
        {
            throw new ServiceException("查询超时（>" + effTimeout + " ms）");
        }
        catch (SQLException e)
        {
            throw new ServiceException("查询失败：" + e.getMessage());
        }
    }

    private static int clamp(Integer raw, int min, int max, int defaultVal)
    {
        if (raw == null || raw <= 0)
        {
            return defaultVal;
        }
        if (raw < min) return min;
        return Math.min(raw, max);
    }

    private static String previewSql(String sql)
    {
        if (sql == null) return "";
        return sql.length() > 1900 ? sql.substring(0, 1900) + "..." : sql;
    }

    private static Object normalize(Object v)
    {
        if (v == null) return null;
        if (v instanceof BigDecimal) return ((BigDecimal) v).doubleValue();
        if (v instanceof Timestamp) return v.toString();
        if (v instanceof byte[]) return "[binary " + ((byte[]) v).length + " bytes]";
        return v;
    }
}
