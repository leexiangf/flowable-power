package com.power.workflow.expense;

import com.power.workflow.constant.WorkflowVars;
import com.power.workflow.support.WorkflowApprovals;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

/**
 * expense 结束节点监听：规范化 approved（Outbox 由全局监听统一投递）。
 */
@Slf4j
@Component("expenseProcessEndListener")
public class ExpenseProcessEndListener implements ExecutionListener {

    @Override
    public void notify(DelegateExecution execution) {
        boolean ok = WorkflowApprovals.toBoolean(execution.getVariable(WorkflowVars.APPROVED), false);
        if ("rejectEnd".equals(execution.getCurrentActivityId())) {
            ok = false;
        }
        execution.setVariable(WorkflowVars.APPROVED, ok);
        log.debug("Expense process ending, pi={}, approved={}",
                execution.getProcessInstanceId(), ok);
    }
}
