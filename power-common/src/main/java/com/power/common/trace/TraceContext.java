package com.power.common.trace;

import org.slf4j.MDC;

public final class TraceContext {

    public static final String TRACE_ID = "traceId";
    public static final String HEADER_TRACE_ID = "X-Trace-Id";

    private TraceContext() {
    }

    public static void setTraceId(String traceId) {
        if (traceId != null && !traceId.isBlank()) {
            MDC.put(TRACE_ID, traceId);
        }
    }

    public static String getTraceId() {
        return MDC.get(TRACE_ID);
    }

    public static void clear() {
        MDC.remove(TRACE_ID);
    }
}
