package com.power.middleware.feign;

import com.power.common.constant.SecurityHeaders;
import com.power.common.trace.TraceContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor powerFeignInterceptor() {
        return (RequestTemplate template) -> {
            String traceId = TraceContext.getTraceId();
            if (traceId != null) {
                template.header(TraceContext.HEADER_TRACE_ID, traceId);
            }
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                String authorization = attrs.getRequest().getHeader(SecurityHeaders.AUTHORIZATION);
                if (authorization != null) {
                    template.header(SecurityHeaders.AUTHORIZATION, authorization);
                }
                String debugAuth = attrs.getRequest().getHeader(SecurityHeaders.DEBUG_AUTH);
                if (debugAuth != null) {
                    template.header(SecurityHeaders.DEBUG_AUTH, debugAuth);
                }
            }
        };
    }
}
