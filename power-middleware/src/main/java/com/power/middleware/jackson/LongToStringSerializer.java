package com.power.middleware.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * 将全部 Long 序列化为字符串，避免前端 JS 精度丢失（雪花 ID）。
 * <p>
 * 反序列化时 Jackson 仍支持 string / number 入参。
 */
public class LongToStringSerializer extends JsonSerializer<Long> {

    @Override
    public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
        } else {
            gen.writeString(value.toString());
        }
    }
}
