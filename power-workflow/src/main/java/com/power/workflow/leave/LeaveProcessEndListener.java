package com.power.workflow.leave;

import com.power.workflow.constant.WorkflowVars;
import com.power.workflow.service.LeaveStatusUpdater;
import com.power.workflow.support.WorkflowApprovals;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

/**
 * BPMN endEvent: delegateExpression="${leaveProcessEndListener}"
 */
@Slf4j
@Component("leaveProcessEndListener")
@RequiredArgsConstructor
public class LeaveProcessEndListener implements ExecutionListener {

    private final LeaveStatusUpdater leaveStatusUpdater;

    @Override
    public void notify(DelegateExecution execution) {
        Long leaveId = WorkflowApprovals.parseLongBusinessKey(execution.getProcessInstanceBusinessKey());
        if (leaveId == null) {
            log.warn("Skip leave status update: invalid businessKey={}",
                    execution.getProcessInstanceBusinessKey());
            return;
        }
        // fail-closed：未设置 approved 视为不通过
        boolean ok = WorkflowApprovals.toBoolean(execution.getVariable(WorkflowVars.APPROVED), false);
        leaveStatusUpdater.markFinished(leaveId, ok);
    }
}
