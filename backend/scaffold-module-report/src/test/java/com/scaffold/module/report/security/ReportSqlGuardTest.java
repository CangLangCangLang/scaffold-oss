package com.scaffold.module.report.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.scaffold.common.exception.ServiceException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportSqlGuardTest
{
    @Test
    @DisplayName("SELECT 起头通过")
    void selectPasses()
    {
        assertDoesNotThrow(() -> ReportSqlGuard.ensureSelectOnly(
                "SELECT id, name FROM sys_user WHERE id > ${minId}"));
    }

    @Test
    @DisplayName("WITH 起头通过（CTE）")
    void withPasses()
    {
        assertDoesNotThrow(() -> ReportSqlGuard.ensureSelectOnly(
                "WITH t AS (SELECT 1 AS x) SELECT * FROM t"));
    }

    @Test
    @DisplayName("DROP 拒")
    void dropRejected()
    {
        ServiceException e = assertThrows(ServiceException.class,
                () -> ReportSqlGuard.ensureSelectOnly("DROP TABLE sys_user"));
        assertTrue(e.getMessage().contains("SELECT"));
    }

    @Test
    @DisplayName("INSERT 拒")
    void insertRejected()
    {
        assertThrows(ServiceException.class,
                () -> ReportSqlGuard.ensureSelectOnly("INSERT INTO t VALUES (1)"));
    }

    @Test
    @DisplayName("UPDATE 拒")
    void updateRejected()
    {
        assertThrows(ServiceException.class,
                () -> ReportSqlGuard.ensureSelectOnly("UPDATE t SET x = 1"));
    }

    @Test
    @DisplayName("DELETE 拒")
    void deleteRejected()
    {
        assertThrows(ServiceException.class,
                () -> ReportSqlGuard.ensureSelectOnly("DELETE FROM t"));
    }

    @Test
    @DisplayName("分号串多语句拒")
    void multipleStatementRejected()
    {
        assertThrows(ServiceException.class,
                () -> ReportSqlGuard.ensureSelectOnly("SELECT 1; DROP TABLE t"));
    }

    @Test
    @DisplayName("尾部分号清理后通过")
    void trailingSemicolonOk()
    {
        assertDoesNotThrow(() -> ReportSqlGuard.ensureSelectOnly("SELECT 1;"));
    }

    @Test
    @DisplayName("行注释里藏 DROP 不应放行（先剥再判）")
    void commentSmugglingRejected()
    {
        // 注释剥掉后剩 SELECT 1 + DROP？— stripCommentsAndStrings 会把 -- 后内容删掉，
        // 所以注释里的 DROP 注释完毕被剥；但若 DROP 在注释外还存在，则拒。
        assertDoesNotThrow(() -> ReportSqlGuard.ensureSelectOnly("SELECT 1 -- DROP TABLE t"));

        assertThrows(ServiceException.class,
                () -> ReportSqlGuard.ensureSelectOnly("SELECT 1 -- ok\nDROP TABLE t"));
    }

    @Test
    @DisplayName("块注释里的 DROP 被剥")
    void blockCommentNoise()
    {
        assertDoesNotThrow(() -> ReportSqlGuard.ensureSelectOnly("SELECT 1 /* DROP TABLE t */"));
    }

    @Test
    @DisplayName("字符串字面量里的 DROP 不应判失败")
    void literalNotMisinterpreted()
    {
        assertDoesNotThrow(() -> ReportSqlGuard.ensureSelectOnly(
                "SELECT id FROM users WHERE name = 'DROP TABLE'"));
    }

    @Test
    @DisplayName("OUTFILE 拒")
    void outfileRejected()
    {
        assertThrows(ServiceException.class,
                () -> ReportSqlGuard.ensureSelectOnly(
                        "SELECT * FROM t INTO OUTFILE '/tmp/x.csv'"));
    }

    @Test
    @DisplayName("LOAD_FILE 拒")
    void loadFileRejected()
    {
        assertThrows(ServiceException.class,
                () -> ReportSqlGuard.ensureSelectOnly(
                        "SELECT LOAD_FILE('/etc/passwd')"));
    }

    @Test
    @DisplayName("@@ 系统变量拒")
    void systemVarRejected()
    {
        assertThrows(ServiceException.class,
                () -> ReportSqlGuard.ensureSelectOnly("SELECT @@version"));
    }

    @Test
    @DisplayName("空 SQL 拒")
    void emptyRejected()
    {
        assertThrows(ServiceException.class, () -> ReportSqlGuard.ensureSelectOnly(""));
        assertThrows(ServiceException.class, () -> ReportSqlGuard.ensureSelectOnly(null));
    }

    @Test
    @DisplayName("纯注释拒")
    void onlyCommentRejected()
    {
        assertThrows(ServiceException.class,
                () -> ReportSqlGuard.ensureSelectOnly("/* hello */"));
    }
}
