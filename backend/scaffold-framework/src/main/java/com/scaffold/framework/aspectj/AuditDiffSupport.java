package com.scaffold.framework.aspectj;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonDiff;
import com.scaffold.common.filter.PropertyPreExcludeFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link AuditLogAspect} 中纯计算部分的工具：
 * <ul>
 *   <li>对象 -&gt; JSON（带敏感字段过滤）</li>
 *   <li>JSON before/after -&gt; RFC 6902 JSON Patch</li>
 *   <li>过长字符串截断</li>
 * </ul>
 * 单独剥离便于单测，无 Spring 上下文依赖。
 *
 * @author scaffold
 */
public final class AuditDiffSupport
{
    private static final Logger log = LoggerFactory.getLogger(AuditDiffSupport.class);

    private AuditDiffSupport() {}

    /** 对象 -&gt; JSON 字符串；过滤掉 excludeFields 列出的顶层属性。 */
    public static String serialize(Object value, String[] excludeFields)
    {
        if (value == null) return null;
        try
        {
            PropertyPreExcludeFilter filter = new PropertyPreExcludeFilter()
                    .addExcludes(excludeFields == null ? new String[0] : excludeFields);
            return JSON.toJSONString(value, filter, JSONWriter.Feature.WriteMapNullValue);
        }
        catch (Exception e)
        {
            log.debug("@AuditLog 序列化失败 type={} reason={}",
                    value.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }

    /** before / after 任意一侧为空都返回 null。失败时 debug 日志 + null。 */
    public static String computeDiff(ObjectMapper mapper, String beforeJson, String afterJson)
    {
        if (mapper == null || beforeJson == null || afterJson == null) return null;
        try
        {
            JsonNode beforeNode = mapper.readTree(beforeJson);
            JsonNode afterNode = mapper.readTree(afterJson);
            JsonNode patch = JsonDiff.asJson(beforeNode, afterNode);
            return patch.toString();
        }
        catch (Exception e)
        {
            log.debug("JsonDiff 失败 reason={}", e.getMessage());
            return null;
        }
    }

    /** 防止超大对象写爆 LONGTEXT 列。 */
    public static String truncate(String s, int maxLength)
    {
        if (s == null) return null;
        return s.length() > maxLength ? s.substring(0, maxLength) : s;
    }
}
