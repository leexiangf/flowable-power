package com.power.workflow.controller;

import com.power.common.result.R;
import com.power.workflow.dto.WorkflowEngineInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Flowable 引擎探活接口。
 */
@Tag(name = "引擎", description = "Flowable Process 引擎探活")
@RestController
@RequestMapping("/workflow")
@RequiredArgsConstructor
public class WorkflowEngineController {

    private final ProcessEngine processEngine;

    /**
     * 查询引擎名称、版本与异步执行器状态。
     *
     * @return 引擎信息视图
     */
    @Operation(summary = "引擎信息", description = "返回引擎名称、版本及异步执行器是否开启，用于健康探活。")
    @GetMapping("/engine/info")
    public R<WorkflowEngineInfoVO> engineInfo() {
        ProcessEngineConfiguration cfg = processEngine.getProcessEngineConfiguration();
        return R.ok(WorkflowEngineInfoVO.builder()
                .engineName(processEngine.getName())
                .version(ProcessEngine.VERSION)
                .asyncExecutorActive(cfg.isAsyncExecutorActivate())
                .build());
    }
}
