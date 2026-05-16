package com.scaffold.framework.aspectj;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class AuditDiffSupportTest
{
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializeNullReturnsNull()
    {
        assertThat(AuditDiffSupport.serialize(null, null)).isNull();
    }

    @Test
    void serializeFiltersSensitiveTopLevelFields()
    {
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("userName", "alice");
        user.put("password", "secret-do-not-log");
        user.put("email", "a@b.com");

        String json = AuditDiffSupport.serialize(user, new String[] { "password" });
        assertThat(json).contains("alice");
        assertThat(json).doesNotContain("secret-do-not-log");
        assertThat(json).doesNotContain("\"password\"");
    }

    @Test
    void computeDiffReturnsNullWhenEitherSideIsNull()
    {
        assertThat(AuditDiffSupport.computeDiff(objectMapper, null, "{}")).isNull();
        assertThat(AuditDiffSupport.computeDiff(objectMapper, "{}", null)).isNull();
    }

    @Test
    void computeDiffEmitsRfc6902Patch() throws Exception
    {
        String before = "{\"userName\":\"alice\",\"status\":\"0\",\"email\":\"a@b.com\"}";
        String after  = "{\"userName\":\"alice\",\"status\":\"1\",\"email\":\"a@b.com\",\"phone\":\"123\"}";
        String patch = AuditDiffSupport.computeDiff(objectMapper, before, after);
        assertThat(patch).isNotNull();

        JsonNode parsed = objectMapper.readTree(patch);
        assertThat(parsed.isArray()).isTrue();
        // 期望至少包含一次 replace（status: 0 -> 1）和一次 add（phone）
        boolean hasReplace = false, hasAdd = false;
        for (JsonNode op : parsed)
        {
            String type = op.path("op").asText();
            String path = op.path("path").asText();
            if ("replace".equals(type) && "/status".equals(path)) hasReplace = true;
            if ("add".equals(type) && "/phone".equals(path)) hasAdd = true;
        }
        assertThat(hasReplace).isTrue();
        assertThat(hasAdd).isTrue();
    }

    @Test
    void computeDiffOnIdenticalReturnsEmptyArray()
    {
        String json = "{\"a\":1}";
        String patch = AuditDiffSupport.computeDiff(objectMapper, json, json);
        assertThat(patch).isEqualTo("[]");
    }

    @Test
    void truncateRespectsLimit()
    {
        assertThat(AuditDiffSupport.truncate(null, 10)).isNull();
        assertThat(AuditDiffSupport.truncate("hello", 10)).isEqualTo("hello");
        assertThat(AuditDiffSupport.truncate("hello world", 5)).isEqualTo("hello");
    }

    @Test
    void serializeMapWithMissingFilterArrayDoesNotThrow()
    {
        Map<String, Object> data = new HashMap<>();
        data.put("k", "v");
        String json = AuditDiffSupport.serialize(data, null);
        assertThat(json).contains("\"k\"").contains("\"v\"");
    }
}
