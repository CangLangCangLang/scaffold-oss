package com.scaffold.common.core.json;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.scaffold.common.annotation.SensitiveLog;
import com.scaffold.common.annotation.SensitiveStrategy;

/**
 * 与 {@link SensitiveLog} 配合使用的 Jackson 序列化器。
 *
 * @author scaffold
 */
public class SensitiveJsonSerializer extends JsonSerializer<String> implements ContextualSerializer
{
    private SensitiveStrategy strategy = SensitiveStrategy.DEFAULT;
    private String mask = "*";
    private int prefixKeep = 0;
    private int suffixKeep = 0;

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        if (value == null)
        {
            gen.writeNull();
            return;
        }
        if (strategy == SensitiveStrategy.CUSTOM)
        {
            gen.writeString(applyCustom(value));
            return;
        }
        gen.writeString(strategy.apply(value, mask));
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException
    {
        if (property == null) return this;
        SensitiveLog annotation = property.getAnnotation(SensitiveLog.class);
        if (annotation == null) annotation = property.getContextAnnotation(SensitiveLog.class);
        if (annotation == null) return prov.findValueSerializer(property.getType(), property);
        SensitiveJsonSerializer serializer = new SensitiveJsonSerializer();
        serializer.strategy = annotation.strategy();
        serializer.mask = annotation.mask();
        serializer.prefixKeep = annotation.prefixKeep();
        serializer.suffixKeep = annotation.suffixKeep();
        return serializer;
    }

    private String applyCustom(String value)
    {
        int total = value.length();
        if (prefixKeep + suffixKeep >= total) return value;
        int maskedLen = total - prefixKeep - suffixKeep;
        return value.substring(0, prefixKeep)
                + SensitiveStrategy.repeat(mask, maskedLen)
                + value.substring(total - suffixKeep);
    }
}
