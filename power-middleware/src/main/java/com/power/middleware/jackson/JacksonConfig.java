package com.power.middleware.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 全局 Jackson：Long 统一输出为字符串，避免前端雪花 ID 精度丢失。
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longToStringJacksonCustomizer() {
        LongToStringSerializer serializer = new LongToStringSerializer();
        return builder -> builder.modulesToInstall(new SimpleModule()
                .addSerializer(Long.class, serializer)
                .addSerializer(long.class, serializer));
    }
}
