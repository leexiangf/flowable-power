package com.power.middleware.datasource;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(MultiDataSourceProperties.class)
@ConditionalOnProperty(prefix = "power.datasource", name = "multi-enabled", havingValue = "true")
public class MultiDataSourceAutoConfiguration {

    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    public static void setDataSource(String key) {
        CONTEXT.set(key);
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static String current() {
        return CONTEXT.get();
    }

    @Bean
    @Primary
    public DataSource dataSource(MultiDataSourceProperties properties) {
        Assert.notEmpty(properties.getHosts(), "power.datasource.hosts must not be empty when multi-enabled");
        Map<Object, Object> target = new HashMap<>();
        properties.getHosts().forEach((name, item) -> {
            HikariDataSource ds = new HikariDataSource();
            ds.setJdbcUrl(item.getUrl());
            ds.setUsername(item.getUsername());
            ds.setPassword(item.getPassword());
            ds.setDriverClassName(item.getDriverClassName());
            ds.setPoolName("power-" + name);
            target.put(name, ds);
        });
        AbstractRoutingDataSource routing = new AbstractRoutingDataSource() {
            @Override
            protected Object determineCurrentLookupKey() {
                String key = current();
                return key == null ? properties.getPrimary() : key;
            }
        };
        routing.setTargetDataSources(target);
        routing.setDefaultTargetDataSource(target.get(properties.getPrimary()));
        routing.afterPropertiesSet();
        return routing;
    }
}
