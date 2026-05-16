package com.scaffold.module.crm.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 验证 M-11 demo form 模板（form_template id=9000，crm_demo_form.sql）的 schema_json
 * 满足 form-create rule[] 协议：
 * <ul>
 *   <li>是合法的 JSON 数组（前端 JSON.parse 必过）</li>
 *   <li>含 4 个字段：customerSource / cooperationStartYear / yearlyContract / extRemark</li>
 *   <li>每个字段都有 type / field / title 三个必填项</li>
 *   <li>customerSource 的 options 都带 value + label</li>
 *   <li>UPDATE 语句里写的 ext_form_data 也是合法 JSON，键集合是上述 4 个字段的子集</li>
 * </ul>
 *
 * <p>SQL 改了 → 这里也要同步更新；前后字符串保持一致是约束。
 * <p>放在 admin 模块跑：crm 模块本身不依赖 form，引用 form_template 表只是 SQL 层面的耦合。
 */
class CrmDemoFormSchemaTest
{
    /** 与 crm_demo_form.sql 第 17 行的 schema_json 字面量保持完全一致 */
    private static final String SCHEMA_JSON =
            "[{\"type\":\"select\",\"field\":\"customerSource\",\"title\":\"客户来源（扩展）\","
                    + "\"value\":\"\",\"props\":{\"placeholder\":\"请选择\",\"clearable\":true},"
                    + "\"options\":[{\"value\":\"OEM\",\"label\":\"OEM 渠道\"},{\"value\":\"AGENT\",\"label\":\"代理商\"},"
                    + "{\"value\":\"DIRECT\",\"label\":\"直销团队\"},{\"value\":\"PARTNER\",\"label\":\"战略合作\"}]},"
                    + "{\"type\":\"input\",\"field\":\"cooperationStartYear\",\"title\":\"合作起始年份\","
                    + "\"value\":null,\"props\":{\"type\":\"number\",\"min\":2000,\"max\":2099,\"placeholder\":\"如 2024\"}},"
                    + "{\"type\":\"switch\",\"field\":\"yearlyContract\",\"title\":\"是否年度合同\","
                    + "\"value\":false,\"props\":{\"activeText\":\"是\",\"inactiveText\":\"否\"}},"
                    + "{\"type\":\"input\",\"field\":\"extRemark\",\"title\":\"扩展备注\",\"value\":\"\","
                    + "\"props\":{\"type\":\"textarea\",\"rows\":3,\"placeholder\":\"客户的特殊约定 / 内部备注\"}}]";

    private static final String EXT_FORM_DATA_BETA =
            "{\"customerSource\":\"AGENT\",\"cooperationStartYear\":2022,\"yearlyContract\":true,"
                    + "\"extRemark\":\"demo - 客户扩展示例 1\"}";

    private static final String EXT_FORM_DATA_GAMMA =
            "{\"customerSource\":\"OEM\",\"cooperationStartYear\":2023,\"yearlyContract\":false,"
                    + "\"extRemark\":\"demo - 客户扩展示例 2\"}";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    @DisplayName("schema_json 是合法 JSON 数组")
    void schemaJsonParsesAsArray() throws Exception
    {
        assertThatCode(() -> MAPPER.readTree(SCHEMA_JSON)).doesNotThrowAnyException();
        assertThat(MAPPER.readTree(SCHEMA_JSON).isArray()).isTrue();
    }

    @Test
    @DisplayName("4 个 widget，每个含 type/field/title 必填项")
    void everyWidgetHasRequiredKeys() throws Exception
    {
        List<Map<String, Object>> rules = MAPPER.readValue(SCHEMA_JSON, new TypeReference<>() {});
        assertThat(rules).hasSize(4);
        for (Map<String, Object> rule : rules)
        {
            assertThat(rule.get("type")).as("type 必填").isNotNull();
            assertThat(rule.get("field")).as("field 必填").isNotNull();
            assertThat(rule.get("title")).as("title 必填").isNotNull();
        }
    }

    @Test
    @DisplayName("4 个字段名跟 ext_form_data 用的 key 完全一致")
    void fieldNamesMatchExtFormDataKeys() throws Exception
    {
        List<Map<String, Object>> rules = MAPPER.readValue(SCHEMA_JSON, new TypeReference<>() {});
        List<String> schemaFields = rules.stream().map(r -> (String) r.get("field")).toList();
        assertThat(schemaFields).containsExactly(
                "customerSource", "cooperationStartYear", "yearlyContract", "extRemark");

        Map<String, Object> beta = MAPPER.readValue(EXT_FORM_DATA_BETA, new TypeReference<>() {});
        assertThat(beta.keySet()).containsExactlyInAnyOrderElementsOf(schemaFields);

        Map<String, Object> gamma = MAPPER.readValue(EXT_FORM_DATA_GAMMA, new TypeReference<>() {});
        assertThat(gamma.keySet()).containsExactlyInAnyOrderElementsOf(schemaFields);
    }

    @Test
    @DisplayName("customerSource 是 select，options 全部带 value + label")
    void selectOptionsAreWellFormed() throws Exception
    {
        List<Map<String, Object>> rules = MAPPER.readValue(SCHEMA_JSON, new TypeReference<>() {});
        Map<String, Object> source = rules.get(0);
        assertThat(source.get("type")).isEqualTo("select");
        assertThat(source.get("field")).isEqualTo("customerSource");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> options = (List<Map<String, Object>>) source.get("options");
        assertThat(options).hasSize(4);
        for (Map<String, Object> opt : options)
        {
            assertThat(opt).containsKeys("value", "label");
            assertThat(opt.get("value")).isInstanceOf(String.class);
            assertThat(opt.get("label")).isInstanceOf(String.class);
        }
    }

    @Test
    @DisplayName("input 类型字段 props.type 与默认值类型互相一致")
    void inputTypePropsAreConsistent() throws Exception
    {
        List<Map<String, Object>> rules = MAPPER.readValue(SCHEMA_JSON, new TypeReference<>() {});
        // index 1: cooperationStartYear（number 输入框，默认 null）
        Map<String, Object> year = rules.get(1);
        assertThat(year.get("type")).isEqualTo("input");
        assertThat(year.get("value")).isNull();
        @SuppressWarnings("unchecked")
        Map<String, Object> yearProps = (Map<String, Object>) year.get("props");
        assertThat(yearProps.get("type")).isEqualTo("number");

        // index 2: yearlyContract（switch，默认 false）
        Map<String, Object> yearly = rules.get(2);
        assertThat(yearly.get("type")).isEqualTo("switch");
        assertThat(yearly.get("value")).isEqualTo(false);

        // index 3: extRemark（textarea 输入框，默认 ""）
        Map<String, Object> remark = rules.get(3);
        assertThat(remark.get("type")).isEqualTo("input");
        assertThat(remark.get("value")).isEqualTo("");
        @SuppressWarnings("unchecked")
        Map<String, Object> remarkProps = (Map<String, Object>) remark.get("props");
        assertThat(remarkProps.get("type")).isEqualTo("textarea");
    }
}
