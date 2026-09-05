package com.power.workflow.leave;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.power.common.constant.ErrorCode;
import com.power.common.exception.BizException;
import com.power.common.model.PageResult;
import com.power.middleware.security.SecurityUtils;
import com.power.workflow.constant.ProcessKeys;
import com.power.workflow.constant.WorkflowVars;
import com.power.workflow.domain.WfLeave;
import com.power.workflow.dto.ProcessInstanceVO;
import com.power.workflow.dto.ProcessStartRequest;
import com.power.workflow.dto.leave.LeaveCreateRequest;
import com.power.workflow.dto.leave.LeaveVO;
import com.power.workflow.mapper.WfLeaveMapper;
import com.power.workflow.service.LeaveStatusUpdater;
import com.power.workflow.service.ProcessInstanceAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 请假业务：创建申请并启动 leave 流程，查询请假单。
 */
@Service
@RequiredArgsConstructor
public class LeaveService {

    /** 审批中 */
    public static final int STATUS_APPROVING = LeaveStatusUpdater.STATUS_APPROVING;

    private final WfLeaveMapper leaveMapper;
    private final ProcessInstanceAppService processInstanceAppService;

    /**
     * 提交请假申请：落库后启动 leave 流程，候选人组为 APPROVER。
     *
     * @param request 请假申请入参
     * @return 请假单视图（含 processInstanceId）
     */
    @Transactional(rollbackFor = Exception.class)
    public LeaveVO create(LeaveCreateRequest request) {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "结束日期不能早于开始日期");
        }

        WfLeave leave = new WfLeave();
        leave.setUserId(userId);
        leave.setUsername(SecurityUtils.currentUsername());
        leave.setDays(request.getDays());
        leave.setReason(request.getReason());
        leave.setStartDate(request.getStartDate());
        leave.setEndDate(request.getEndDate());
        leave.setStatus(STATUS_APPROVING);
        leaveMapper.insert(leave);

        ProcessStartRequest start = new ProcessStartRequest();
        start.setProcessDefinitionKey(ProcessKeys.LEAVE);
        start.setBusinessKey(String.valueOf(leave.getId()));
        start.setTitle("请假-" + leave.getUsername() + "-" + leave.getDays() + "天");
        Map<String, Object> vars = new HashMap<>();
        vars.put(WorkflowVars.BUSINESS_TYPE, WorkflowVars.BUSINESS_TYPE_LEAVE);
        vars.put("leaveId", leave.getId());
        vars.put("days", leave.getDays());
        vars.put("reason", leave.getReason());
        start.setVariables(vars);
        if (request.getCcUserIds() != null && !request.getCcUserIds().isEmpty()) {
            start.setCcUserIds(request.getCcUserIds());
        }

        ProcessInstanceVO pi = processInstanceAppService.startFromBusiness(start);
        leave.setProcessInstanceId(pi.getId());
        leaveMapper.updateById(leave);
        return toVo(leave);
    }

    /**
     * 分页查询当前用户的请假申请。
     */
    public PageResult<LeaveVO> listMine(long pageNum, long pageSize) {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        Page<WfLeave> page = leaveMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<WfLeave>()
                        .eq(WfLeave::getUserId, userId)
                        .orderByDesc(WfLeave::getCreateTime));
        return PageResult.of(
                page.getRecords().stream().map(this::toVo).toList(),
                page.getTotal(),
                pageNum,
                pageSize);
    }

    /**
     * 按主键查询请假单详情。
     *
     * @param id 请假单 ID
     * @return 请假单视图
     */
    public LeaveVO detail(Long id) {
        WfLeave leave = leaveMapper.selectById(id);
        if (leave == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "请假单不存在");
        }
        return toVo(leave);
    }

    /**
     * 实体转视图。
     *
     * @param leave 请假实体
     * @return 请假视图
     */
    private LeaveVO toVo(WfLeave leave) {
        return LeaveVO.builder()
                .id(leave.getId())
                .userId(leave.getUserId())
                .username(leave.getUsername())
                .days(leave.getDays())
                .reason(leave.getReason())
                .startDate(leave.getStartDate())
                .endDate(leave.getEndDate())
                .status(leave.getStatus())
                .processInstanceId(leave.getProcessInstanceId())
                .createTime(leave.getCreateTime())
                .build();
    }
}
