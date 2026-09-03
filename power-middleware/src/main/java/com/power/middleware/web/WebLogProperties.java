package com.power.middleware.web;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * HTTP 接口调试日志（仅建议在 local/dev 开启）。
 */
@Data
@ConfigurationProperties(prefix = "power.web")
public class WebLogProperties {

    /**
     * 是否打印请求/响应摘要；生产环境务必保持 false。
     */
    private boolean apiLogEnabled = false;

    /**
     * 请求/响应体最大打印长度（字符），超出截断。
     */
    private int maxBodyLength = 2048;

    /**
     * 跳过的路径（Ant 风格），默认忽略文档与探活。
     */
    private List<String> excludePaths = new ArrayList<>(List.of(
            "/actuator/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/favicon.ico"
    ));
}
