package com.scaffold.common.core.dict;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.scaffold.common.annotation.DictTrans;
import com.scaffold.common.utils.DictUtils;
import com.scaffold.common.utils.StringUtils;

/**
 * {@link DictTrans} 配套 Jackson 序列化器。
 * 在写出原字段值之外，再追加一个伴生字段（默认在原字段名后加 Label）作为可读标签。
 *
 * @author scaffold
 */
public class DictTransSerializer extends JsonSerializer<Object> implements ContextualSerializer
{
    private final String dictType;

    private final String targetField;

    private final String separator;

    public DictTransSerializer()
    {
        this(null, null, ",");
    }

    private DictTransSerializer(String dictType, String targetField, String separator)
    {
        this.dictType = dictType;
        this.targetField = targetField;
        this.separator = separator;
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        gen.writeObject(value);
        if (value == null || dictType == null)
        {
            return;
        }
        String raw = String.valueOf(value);
        if (StringUtils.isEmpty(raw))
        {
            return;
        }
        String label = DictUtils.getDictLabel(dictType, raw, separator);
        gen.writeFieldName(targetField);
        gen.writeString(label == null ? "" : label);
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException
    {
        if (property == null)
        {
            return prov.findNullValueSerializer(null);
        }
        DictTrans annotation = property.getAnnotation(DictTrans.class);
        if (annotation == null)
        {
            annotation = property.getContextAnnotation(DictTrans.class);
        }
        String type = annotation == null ? null : annotation.type();
        String customTarget = annotation == null ? "" : annotation.target();
        String resolvedTarget = StringUtils.isEmpty(customTarget) ? property.getName() + "Label" : customTarget;
        String sep = annotation == null ? "," : annotation.separator();
        return new DictTransSerializer(type, resolvedTarget, sep);
    }
}
