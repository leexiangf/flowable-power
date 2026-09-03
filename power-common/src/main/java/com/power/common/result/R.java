package com.power.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.power.common.constant.ErrorCode;
import com.power.common.trace.TraceContext;
import lombok.Data;

import java.io.Serializable;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private int code;
    private String message;
    private T data;
    private String traceId;
    /** Only filled when power.exception.include-source=true */
    private String source;

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setCode(ErrorCode.SUCCESS.getCode());
        r.setMessage(ErrorCode.SUCCESS.getMessage());
        r.setData(data);
        r.setTraceId(TraceContext.getTraceId());
        return r;
    }

    public static <T> R<T> fail(ErrorCode errorCode) {
        return fail(errorCode.getCode(), errorCode.getMessage());
    }

    public static <T> R<T> fail(int code, String message) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMessage(message);
        r.setTraceId(TraceContext.getTraceId());
        return r;
    }

    public R<T> source(String source) {
        this.source = source;
        return this;
    }
}
