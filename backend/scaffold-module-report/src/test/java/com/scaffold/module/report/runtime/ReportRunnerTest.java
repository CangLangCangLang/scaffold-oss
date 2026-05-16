package com.scaffold.module.report.runtime;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.scaffold.common.exception.ServiceException;
import com.scaffold.module.report.dto.RunResult;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ReportRunner 集成测试：H2 内存库验证 select-only / 行数 / 超时三闸 + 类型映射。
 */
class ReportRunnerTest
{
    private JdbcDataSource ds;
    private ReportRunner runner;

    @BeforeEach
    void setUp() throws Exception
    {
        ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:report_runner_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        try (Connection c = ds.getConnection())
        {
            c.createStatement().execute("CREATE TABLE t (id INT PRIMARY KEY, name VARCHAR(64), price DECIMAL(10,2))");
            for (int i = 1; i <= 50; i++)
            {
                c.createStatement().execute(
                        "INSERT INTO t VALUES (" + i + ", 'name-" + i + "', " + (i * 1.5) + ")");
            }
        }

        ReportDataSourceManager mgr = mock(ReportDataSourceManager.class);
        when(mgr.resolve(0L)).thenReturn(ds);
        when(mgr.resolve(null)).thenReturn(ds);

        runner = new ReportRunner(mgr, 10000, 30000, 3000L);
    }

    @AfterEach
    void tearDown() throws Exception
    {
        try (Connection c = ds.getConnection())
        {
            c.createStatement().execute("DROP ALL OBJECTS");
        }
    }

    @Test
    @DisplayName("简单 SELECT 返回所有列")
    void simpleSelect()
    {
        RunResult r = runner.execute(0L, "SELECT id, name FROM t WHERE id = ${id}",
                Map.of("id", 1), null, null);
        assertEquals(2, r.getColumns().size());
        assertEquals(1, r.getRowCount());
        assertEquals(1, r.getRows().get(0).get(0));
    }

    @Test
    @DisplayName("行数截断生效")
    void rowLimitTruncates()
    {
        RunResult r = runner.execute(0L, "SELECT id FROM t ORDER BY id", Map.of(), 10, null);
        assertEquals(10, r.getRowCount());
        assertTrue(r.isTruncated());
    }

    @Test
    @DisplayName("行数未达上限不算截断")
    void rowLimitNotReached()
    {
        RunResult r = runner.execute(0L, "SELECT id FROM t WHERE id <= ${cap} ORDER BY id",
                Map.of("cap", 5), 100, null);
        assertEquals(5, r.getRowCount());
        assertFalse(r.isTruncated());
    }

    @Test
    @DisplayName("BigDecimal 自动转 Double 给前端友好")
    void decimalNormalized()
    {
        RunResult r = runner.execute(0L, "SELECT price FROM t WHERE id = ${id}",
                Map.of("id", 4), null, null);
        Object price = r.getRows().get(0).get(0);
        assertEquals(Double.class, price.getClass());
        assertEquals(6.0, (Double) price, 0.001);
    }

    @Test
    @DisplayName("DROP 走不进来：模板被 SqlGuard 直接拒")
    void runnerStillCallsGuard()
    {
        ServiceException e = assertThrows(ServiceException.class, () ->
                runner.execute(0L, "DROP TABLE t", null, null, null));
        assertTrue(e.getMessage().contains("SELECT"));
    }

    @Test
    @DisplayName("JDBC 异常（不存在表）被映射为 ServiceException")
    void sqlExceptionWrapped()
    {
        ServiceException e = assertThrows(ServiceException.class, () ->
                runner.execute(0L, "SELECT * FROM no_such_table", null, null, null));
        assertTrue(e.getMessage().contains("查询失败"));
    }

    @Test
    @DisplayName("缺少必填参数：在 ParamBinder 阶段先报")
    void missingParam()
    {
        Map<String, Object> empty = new HashMap<>();
        ServiceException e = assertThrows(ServiceException.class, () ->
                runner.execute(0L, "SELECT id FROM t WHERE id = ${id}", empty, null, null));
        assertTrue(e.getMessage().contains("id"));
    }

    @Test
    @DisplayName("即席 SQL：无参数也能跑")
    void noParam()
    {
        RunResult r = runner.execute(0L, "SELECT COUNT(*) FROM t", null, null, null);
        assertEquals(1, r.getRowCount());
        assertEquals(50, ((Number) r.getRows().get(0).get(0)).intValue());
    }
}
