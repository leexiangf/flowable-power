package com.power.middleware.datasource;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lightweight multi-datasource switch. Disabled unless {@code multi-enabled=true}.
 */
@Data
@ConfigurationProperties(prefix = "power.datasource")
public class MultiDataSourceProperties {

    private boolean multiEnabled = false;

    private String primary = "master";

    private Map<String, DataSourceItem> hosts = new LinkedHashMap<>();

    @Data
    public static class DataSourceItem {
        private String url;
        private String username;
        private String password;
        private String driverClassName = "com.mysql.cj.jdbc.Driver";
    }
}
