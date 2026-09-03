package com.power.workflow.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.power.workflow.domain.WfLeave;
import com.power.workflow.mapper.WfLeaveMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 请假单状态回写（流程结束监听器 / 撤销调用）。
 */
@Service
@RequiredArgsConstructor
public class LeaveStatusUpdater {

    /** 审批中 */
    public static final int STATUS_APPROVING = 1;

    /** 审批通过 */
    public static final int STATUS_APPROVED = 2;

    /** 审批驳回 */
    public static final int STATUS_REJECTED = 3;

    /** 发起人撤销 */
    public static final int STATUS_CANCELLED = 4;

    private final WfLeaveMapper leaveMapper;

    /**
     * 流程结束后更新请假单状态（仅审批中可变更，幂等）。
     *
     * @param leaveId  请假单 ID（businessKey）
     * @param approved 是否通过
     */
    @Transactional(rollbackFor = Exception.class)
    public void markFinished(Long leaveId, boolean approved) {
        if (leaveId == null) {
            return;
        }
        leaveMapper.update(null, new LambdaUpdateWrapper<WfLeave>()
                .eq(WfLeave::getId, leaveId)
                .eq(WfLeave::getStatus, STATUS_APPROVING)
                .set(WfLeave::getStatus, approved ? STATUS_APPROVED : STATUS_REJECTED));
    }

    /**
     * 发起人撤销后更新请假单状态（仅审批中可变更，幂等）。
     *
     * @param leaveId 请假单 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void markCancelled(Long leaveId) {
        if (leaveId == null) {
            return;
        }
        leaveMapper.update(null, new LambdaUpdateWrapper<WfLeave>()
                .eq(WfLeave::getId, leaveId)
                .eq(WfLeave::getStatus, STATUS_APPROVING)
                .set(WfLeave::getStatus, STATUS_CANCELLED));
    }
}
