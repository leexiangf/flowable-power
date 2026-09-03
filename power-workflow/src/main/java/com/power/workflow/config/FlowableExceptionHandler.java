package com.power.workflow.config;

import com.power.common.constant.ErrorCode;
import com.power.common.result.R;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Flowable 引擎异常映射（仅 power-workflow）。
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class FlowableExceptionHandler {

    @ExceptionHandler(FlowableOptimisticLockingException.class)
    public R<Void> handleOptimistic(FlowableOptimisticLockingException ex) {
        log.warn("Flowable optimistic lock: {}", ex.getMessage());
        return R.fail(ErrorCode.BAD_REQUEST.getCode(), "任务状态已变更，请刷新后重试");
    }

    @ExceptionHandler(FlowableObjectNotFoundException.class)
    public R<Void> handleNotFound(FlowableObjectNotFoundException ex) {
        log.warn("Flowable object not found: {}", ex.getMessage());
        return R.fail(ErrorCode.NOT_FOUND.getCode(), "流程任务或实例不存在");
    }

    @ExceptionHandler(FlowableException.class)
    public R<Void> handleFlowable(FlowableException ex) {
        log.warn("Flowable exception: {}", ex.getMessage());
        return R.fail(ErrorCode.BAD_REQUEST.getCode(),
                ex.getMessage() == null ? "流程引擎操作失败" : ex.getMessage());
    }
}
