package com.scaffold.module.report.security;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.scaffold.common.exception.ServiceException;

/**
 * 参数绑定器：把模板里 {@code ${name}} 占位 → JDBC 标准 {@code ?} 占位 + 顺序参数列表。
 *
 * <p>始终走 PreparedStatement，杜绝拼接 SQL 注入。{@code ${name}} 形态对前端友好且不会与 SQL 关键字冲突。</p>
 *
 * @author scaffold
 */
public final class ReportParamBinder
{
    /** ${name}：name 由字母数字下划线组成，最长 64 */
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([A-Za-z_][A-Za-z0-9_]{0,63})\\}");

    private ReportParamBinder()
    {
    }

    public static Result bind(String templateSql, Map<String, Object> params)
    {
        Matcher m = PLACEHOLDER.matcher(templateSql);
        StringBuilder out = new StringBuilder();
        List<Object> values = new ArrayList<>();
        Set<String> missing = new LinkedHashSet<>();
        int last = 0;
        while (m.find())
        {
            out.append(templateSql, last, m.start());
            String key = m.group(1);
            if (params == null || !params.containsKey(key))
            {
                missing.add(key);
                out.append("?");
                values.add(null);
            }
            else
            {
                out.append("?");
                values.add(params.get(key));
            }
            last = m.end();
        }
        out.append(templateSql, last, templateSql.length());

        if (!missing.isEmpty())
        {
            throw new ServiceException("缺少必填参数：" + String.join(", ", missing));
        }
        return new Result(out.toString(), values);
    }

    public static final class Result
    {
        private final String sql;
        private final List<Object> values;

        Result(String sql, List<Object> values)
        {
            this.sql = sql;
            this.values = values;
        }

        public String getSql()
        {
            return sql;
        }

        public List<Object> getValues()
        {
            return values;
        }
    }
}
