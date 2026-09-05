package com.power.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 启动流程实例请求。
 */
@Data
@Schema(description = "启动流程实例请求")
public class ProcessStartRequest {

    /** 流程定义 key，如 leave */
    @Schema(description = "流程定义 key", example = "leave")
    private String processDefinitionKey;

    /** 业务主键（字符串） */
    @Schema(description = "业务主键", example = "1234567890")
    private String businessKey;

    /** 流程标题（写入变量 title） */
    @Schema(description = "流程标题", example = "请假-张三-1天")
    private String title;

    /** 额外流程变量 */
    @Schema(description = "流程变量")
    private Map<String, Object> variables = new HashMap<>();

    /** 抄送人 userId 列表 */
    @Schema(description = "抄送人 userId 列表")
    private List<String> ccUserIds = new ArrayList<>();
}
