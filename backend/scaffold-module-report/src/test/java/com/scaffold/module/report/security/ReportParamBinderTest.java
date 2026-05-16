package com.scaffold.module.report.security;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.scaffold.common.exception.ServiceException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportParamBinderTest
{
    @Test
    @DisplayName("无占位符直接通过")
    void plainSqlReturnsAsIs()
    {
        ReportParamBinder.Result r = ReportParamBinder.bind("SELECT 1", null);
        assertEquals("SELECT 1", r.getSql());
        assertTrue(r.getValues().isEmpty());
    }

    @Test
    @DisplayName("一个占位符替换为 ? 并按顺序绑值")
    void singleParamBound()
    {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("id", 42);
        ReportParamBinder.Result r = ReportParamBinder.bind("SELECT * FROM t WHERE id = ${id}", p);
        assertEquals("SELECT * FROM t WHERE id = ?", r.getSql());
        assertEquals(1, r.getValues().size());
        assertEquals(42, r.getValues().get(0));
    }

    @Test
    @DisplayName("多个占位符按文中顺序绑值")
    void multiParamsOrdered()
    {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("min", 1);
        p.put("max", 100);
        ReportParamBinder.Result r = ReportParamBinder.bind(
                "SELECT * FROM t WHERE x BETWEEN ${min} AND ${max}", p);
        assertEquals("SELECT * FROM t WHERE x BETWEEN ? AND ?", r.getSql());
        assertEquals(1, r.getValues().get(0));
        assertEquals(100, r.getValues().get(1));
    }

    @Test
    @DisplayName("同一占位符多次出现被独立绑两次")
    void sameKeyTwice()
    {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("k", "abc");
        ReportParamBinder.Result r = ReportParamBinder.bind(
                "SELECT * FROM t WHERE a = ${k} OR b = ${k}", p);
        assertEquals("SELECT * FROM t WHERE a = ? OR b = ?", r.getSql());
        assertEquals(2, r.getValues().size());
    }

    @Test
    @DisplayName("缺失参数被报告")
    void missingReported()
    {
        ServiceException e = assertThrows(ServiceException.class, () ->
                ReportParamBinder.bind("SELECT ${a}, ${b}", Map.of("a", 1)));
        assertTrue(e.getMessage().contains("b"));
    }
}
