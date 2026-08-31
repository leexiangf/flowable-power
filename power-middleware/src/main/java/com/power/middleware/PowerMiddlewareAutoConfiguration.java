package com.power.middleware;

import com.power.middleware.datasource.MultiDataSourceProperties;
import com.power.middleware.exception.ExceptionProperties;
import com.power.middleware.security.SecurityProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@AutoConfiguration
@EnableScheduling
@EnableConfigurationProperties({
        SecurityProperties.class,
        ExceptionProperties.class,
        MultiDataSourceProperties.class
})
@ComponentScan(basePackages = "com.power.middleware")
@MapperScan("com.power.middleware.mq.outbox")
public class PowerMiddlewareAutoConfiguration {
}
