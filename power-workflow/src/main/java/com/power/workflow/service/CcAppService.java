package com.power.workflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.power.common.model.PageResult;
import com.power.workflow.constant.WorkflowVars;
import com.power.workflow.domain.WfCc;
import com.power.workflow.dto.CcVO;
import com.power.workflow.mapper.WfCcMapper;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 流程抄送：写入与查询（只读，不产生待办）。
 */
@Service
@RequiredArgsConstructor
public class CcAppService {

    private final WfCcMapper ccMapper;
    private final HistoryService historyService;
    private final WorkflowIdentityFacade workflowIdentityFacade;

    /**
     * 批量写入抄送记录（自动去重、跳过本人）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void record(String processInstanceId, String taskId, List<String> ccUserIds, Long fromUserId) {
        if (!StringUtils.hasText(processInstanceId) || fromUserId == null
                || ccUserIds == null || ccUserIds.isEmpty()) {
            return;
        }
        Set<Long> targetIds = new HashSet<>();
        for (String uid : ccUserIds) {
            if (!StringUtils.hasText(uid)) {
                continue;
            }
            try {
                Long id = Long.valueOf(uid.trim());
                if (!Objects.equals(id, fromUserId)) {
                    targetIds.add(id);
                }
            } catch (NumberFormatException ignored) {
                // skip invalid id
            }
        }
        if (targetIds.isEmpty()) {
            return;
        }
        for (Long userId : targetIds) {
            if (exists(processInstanceId, taskId, userId)) {
                continue;
            }
            WfCc cc = new WfCc();
            cc.setProcessInstanceId(processInstanceId);
            cc.setTaskId(StringUtils.hasText(taskId) ? taskId : null);
            cc.setUserId(userId);
            cc.setFromUserId(fromUserId);
            cc.setReadFlag(0);
            ccMapper.insert(cc);
        }
    }

    /**
     * 我的抄送分页。
     */
    public PageResult<CcVO> listMine(long pageNum, long pageSize, Long userId) {
        Page<WfCc> page = ccMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<WfCc>()
                        .eq(WfCc::getUserId, userId)
                        .orderByDesc(WfCc::getCreateTime));
        List<CcVO> records = page.getRecords().stream().map(this::toVo).collect(Collectors.toList());
        return PageResult.of(records, page.getTotal(), pageNum, pageSize);
    }

    /**
     * 是否为抄送接收人。
     */
    public boolean isRecipient(String processInstanceId, Long userId) {
        if (!StringUtils.hasText(processInstanceId) || userId == null) {
            return false;
        }
        return ccMapper.selectCount(new LambdaQueryWrapper<WfCc>()
                .eq(WfCc::getProcessInstanceId, processInstanceId)
                .eq(WfCc::getUserId, userId)) > 0;
    }

    /**
     * 标记已读。
     */
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long ccId, Long userId) {
        WfCc cc = ccMapper.selectById(ccId);
        if (cc == null || !Objects.equals(cc.getUserId(), userId)) {
            return;
        }
        if (Objects.equals(cc.getReadFlag(), 1)) {
            return;
        }
        cc.setReadFlag(1);
        ccMapper.updateById(cc);
    }

    private boolean exists(String processInstanceId, String taskId, Long userId) {
        LambdaQueryWrapper<WfCc> wrapper = new LambdaQueryWrapper<WfCc>()
                .eq(WfCc::getProcessInstanceId, processInstanceId)
                .eq(WfCc::getUserId, userId);
        if (StringUtils.hasText(taskId)) {
            wrapper.eq(WfCc::getTaskId, taskId);
        } else {
            wrapper.isNull(WfCc::getTaskId);
        }
        return ccMapper.selectCount(wrapper) > 0;
    }

    private CcVO toVo(WfCc cc) {
        HistoricProcessInstance historic = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(cc.getProcessInstanceId())
                .includeProcessVariables()
                .singleResult();
        String title = null;
        String defKey = null;
        String businessKey = null;
        boolean ended = true;
        if (historic != null) {
            defKey = historic.getProcessDefinitionKey();
            businessKey = historic.getBusinessKey();
            ended = historic.getEndTime() != null;
            Map<String, Object> vars = historic.getProcessVariables();
            if (vars != null && vars.get(WorkflowVars.TITLE) != null) {
                title = String.valueOf(vars.get(WorkflowVars.TITLE));
            }
        }
        return CcVO.builder()
                .id(String.valueOf(cc.getId()))
                .processInstanceId(cc.getProcessInstanceId())
                .taskId(cc.getTaskId())
                .processDefinitionKey(defKey)
                .title(title)
                .businessKey(businessKey)
                .fromUserId(cc.getFromUserId())
                .fromUserName(workflowIdentityFacade.resolveDisplayName(String.valueOf(cc.getFromUserId())))
                .readFlag(cc.getReadFlag())
                .createTime(cc.getCreateTime())
                .ended(ended)
                .build();
    }
}
