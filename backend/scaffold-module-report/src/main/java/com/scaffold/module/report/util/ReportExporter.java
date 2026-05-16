package com.scaffold.module.report.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import com.scaffold.module.report.dto.RunResult;

/**
 * 动态列报表导出（CSV / xlsx），不依赖实体注解。
 *
 * <ul>
 *   <li>CSV：UTF-8 BOM 起头（Excel 中文友好），逗号分隔，{@code "} 字段双引号转义。</li>
 *   <li>XLSX：POI SXSSFWorkbook 流式写，避免大数据 OOM。</li>
 * </ul>
 *
 * @author scaffold
 */
public final class ReportExporter
{
    private ReportExporter()
    {
    }

    public static void writeCsv(RunResult result, OutputStream os) throws IOException
    {
        os.write(0xEF); os.write(0xBB); os.write(0xBF); // BOM
        try (Writer w = new OutputStreamWriter(os, StandardCharsets.UTF_8))
        {
            List<String> cols = result.getColumns();
            for (int i = 0; i < cols.size(); i++)
            {
                if (i > 0) w.write(',');
                w.write(escapeCsv(cols.get(i)));
            }
            w.write('\n');
            for (List<Object> row : result.getRows())
            {
                for (int i = 0; i < row.size(); i++)
                {
                    if (i > 0) w.write(',');
                    Object v = row.get(i);
                    w.write(escapeCsv(v == null ? "" : v.toString()));
                }
                w.write('\n');
            }
            w.flush();
        }
    }

    public static void writeXlsx(RunResult result, OutputStream os) throws IOException
    {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(1000))
        {
            Sheet sh = wb.createSheet("report");
            List<String> cols = result.getColumns();
            Row header = sh.createRow(0);
            for (int i = 0; i < cols.size(); i++)
            {
                Cell c = header.createCell(i);
                c.setCellValue(cols.get(i));
            }
            int r = 1;
            for (List<Object> row : result.getRows())
            {
                Row line = sh.createRow(r++);
                for (int i = 0; i < row.size(); i++)
                {
                    Cell c = line.createCell(i);
                    Object v = row.get(i);
                    if (v == null)
                    {
                        c.setBlank();
                    }
                    else if (v instanceof Number)
                    {
                        c.setCellValue(((Number) v).doubleValue());
                    }
                    else if (v instanceof Boolean)
                    {
                        c.setCellValue((Boolean) v);
                    }
                    else
                    {
                        c.setCellValue(v.toString());
                    }
                }
            }
            wb.write(os);
            wb.dispose();
        }
    }

    private static String escapeCsv(String s)
    {
        if (s == null) return "";
        boolean needQuote = s.indexOf(',') >= 0 || s.indexOf('"') >= 0
                || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0;
        String escaped = s.replace("\"", "\"\"");
        return needQuote ? "\"" + escaped + "\"" : escaped;
    }
}
