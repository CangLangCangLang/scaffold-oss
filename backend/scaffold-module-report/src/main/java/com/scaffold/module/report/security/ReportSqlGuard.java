package com.scaffold.module.report.security;

import java.util.Locale;
import java.util.regex.Pattern;
import com.scaffold.common.exception.ServiceException;

/**
 * SQL 安全护栏：保证用户提交的报表模板 SQL 是只读 SELECT。
 *
 * <h3>规则（按顺序执行）</h3>
 * <ol>
 *   <li>剥离所有注释（{@code -- ...}, {@code # ...}, {@code /* ... *​/}）以防注释走私 DDL/DML。</li>
 *   <li>剥离字符串字面量（避免字面量里出现关键字误判）。</li>
 *   <li>SQL 主体必须以 SELECT 或 WITH 起头。</li>
 *   <li>整体 ≤ 1 条语句（不允许分号串多条）。</li>
 *   <li>禁止出现写 / DDL / 控制 关键字：INSERT/UPDATE/DELETE/MERGE/REPLACE/UPSERT、CREATE/DROP/ALTER/RENAME/TRUNCATE、GRANT/REVOKE、CALL/EXEC/EXECUTE、LOAD/HANDLER、LOCK/UNLOCK、SET、DO。</li>
 *   <li>禁止 INTO OUTFILE / DUMPFILE / LOAD_FILE（防止读写本地磁盘）。</li>
 *   <li>未声明的参数变量（{@code ${name}}）以外不允许 {@code @@} 系统变量。</li>
 * </ol>
 *
 * <p>不强行 join AST 解析器，规则集足够覆盖 90% 攻击面，且不依赖外部 jar。</p>
 *
 * @author scaffold
 */
public final class ReportSqlGuard
{
    /** 命中即拒的写 / DDL / 控制关键字（按词边界匹配） */
    private static final Pattern FORBIDDEN_WORDS = Pattern.compile(
            "\\b(INSERT|UPDATE|DELETE|MERGE|REPLACE|UPSERT|"
                    + "CREATE|DROP|ALTER|RENAME|TRUNCATE|"
                    + "GRANT|REVOKE|"
                    + "CALL|EXEC|EXECUTE|"
                    + "LOAD|HANDLER|"
                    + "LOCK|UNLOCK|"
                    + "SET|DO)\\b",
            Pattern.CASE_INSENSITIVE);

    /** 文件读写关键字（更严格） */
    private static final Pattern FORBIDDEN_FILE_OPS = Pattern.compile(
            "\\b(OUTFILE|DUMPFILE|LOAD_FILE)\\b",
            Pattern.CASE_INSENSITIVE);

    /** 起头允许 SELECT / WITH */
    private static final Pattern HEAD_SELECT_OR_WITH = Pattern.compile(
            "^(SELECT|WITH)\\b",
            Pattern.CASE_INSENSITIVE);

    /** 块注释 /* … *​/ */
    private static final Pattern BLOCK_COMMENT = Pattern.compile(
            "/\\*[\\s\\S]*?\\*/");

    /** 行注释 -- 或 # 至行尾 */
    private static final Pattern LINE_COMMENT = Pattern.compile(
            "(--|#)[^\\n]*");

    /** 字符串字面量 'xxx' or "xxx"（含转义） */
    private static final Pattern STRING_LITERAL = Pattern.compile(
            "'(?:''|\\\\.|[^'\\\\])*'|\"(?:\"\"|\\\\.|[^\"\\\\])*\"");

    private ReportSqlGuard()
    {
    }

    /**
     * 校验入参 SQL，违规直接抛出 ServiceException。
     *
     * @param sql 用户提交的模板 SQL
     */
    public static void ensureSelectOnly(String sql)
    {
        if (sql == null || sql.trim().isEmpty())
        {
            throw new ServiceException("SQL 不能为空");
        }
        String stripped = stripCommentsAndStrings(sql).trim();
        if (stripped.isEmpty())
        {
            throw new ServiceException("SQL 仅由注释组成，已拒绝");
        }

        if (stripped.endsWith(";"))
        {
            stripped = stripped.substring(0, stripped.length() - 1).trim();
        }

        if (stripped.contains(";"))
        {
            throw new ServiceException("不允许多条 SQL（检测到分号分隔）");
        }

        if (!HEAD_SELECT_OR_WITH.matcher(stripped).find())
        {
            throw new ServiceException("仅允许 SELECT / WITH 起始的只读查询");
        }

        if (FORBIDDEN_WORDS.matcher(stripped).find())
        {
            throw new ServiceException("SQL 含写 / DDL / 控制关键字，已拒绝");
        }

        if (FORBIDDEN_FILE_OPS.matcher(stripped).find())
        {
            throw new ServiceException("SQL 含文件读写操作（OUTFILE / LOAD_FILE 等），已拒绝");
        }

        if (stripped.toUpperCase(Locale.ROOT).contains("@@"))
        {
            throw new ServiceException("SQL 不允许使用 @@ 系统变量");
        }
    }

    /**
     * 剥离注释与字符串字面量后再做关键字检测。
     *
     * <p>顺序：先去注释（防注释里藏字符串），再去字符串。</p>
     */
    static String stripCommentsAndStrings(String sql)
    {
        String s = BLOCK_COMMENT.matcher(sql).replaceAll(" ");
        s = LINE_COMMENT.matcher(s).replaceAll(" ");
        s = STRING_LITERAL.matcher(s).replaceAll("''");
        return s;
    }
}
