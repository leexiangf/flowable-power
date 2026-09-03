package com.power.workflow.constant;

/**
 * 工作流 MQ / Outbox 主题约定。
 */
public final class WorkflowMqTopics {

    /** RabbitMQ Topic Exchange */
    public static final String EXCHANGE = "power.workflow";

    /** 流程正常结束 */
    public static final String TAG_PROCESS_COMPLETED = "process.completed";

    /** 流程撤销 / 删除 */
    public static final String TAG_PROCESS_CANCELLED = "process.cancelled";

    private WorkflowMqTopics() {
    }
}
