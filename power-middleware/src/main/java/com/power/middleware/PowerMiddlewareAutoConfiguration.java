package com.power.middleware;

import com.power.middleware.datasource.MultiDataSourceProperties;
import com.power.middleware.exception.ExceptionProperties;
import com.power.middleware.mq.RabbitMqProperties;
import com.power.middleware.redis.RedisEnhancementProperties;
import com.power.middleware.security.SecurityProperties;
import com.power.middleware.web.WebLogProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@EnableConfigurationProperties({
        SecurityProperties.class,
        ExceptionProperties.class,
        MultiDataSourceProperties.class,
        RabbitMqProperties.class,
        RedisEnhancementProperties.class,
        WebLogProperties.class
})
@ComponentScan(basePackages = "com.power.middleware")
@MapperScan("com.power.middleware.mq.outbox")
public class PowerMiddlewareAutoConfiguration {
}
