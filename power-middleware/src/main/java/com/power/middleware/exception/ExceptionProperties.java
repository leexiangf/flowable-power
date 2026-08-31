package com.power.middleware.exception;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "power.exception")
public class ExceptionProperties {

    /** When true, response may include exception class#method:line (local/dev only). */
    private boolean includeSource = false;
}
