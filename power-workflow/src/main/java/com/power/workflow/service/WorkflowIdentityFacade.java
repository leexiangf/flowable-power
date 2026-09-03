package com.power.workflow.service;

import com.power.common.constant.ErrorCode;
import com.power.common.exception.BizException;
import com.power.common.result.R;
import com.power.workflow.dto.WorkflowUserView;
import com.power.workflow.feign.AuthWorkflowIdentityClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作流身份门面：角色编码与用户展示名。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowIdentityFacade {

    private final AuthWorkflowIdentityClient authWorkflowIdentityClient;
    private final Map<Long, String> displayNameCache = new ConcurrentHashMap<>();

    /**
     * 查询用户启用中的角色编码。
     */
    public List<String> listRoleCodes(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        try {
            R<List<String>> resp = authWorkflowIdentityClient.listRoleCodes(userId);
            if (resp == null || resp.getCode() != ErrorCode.SUCCESS.getCode() || resp.getData() == null) {
                log.warn("Load role codes failed for userId={}, resp={}", userId, resp);
                return Collections.emptyList();
            }
            return resp.getData();
        } catch (Exception ex) {
            log.warn("Feign role codes failed for userId={}: {}", userId, ex.getMessage());
            throw new BizException(ErrorCode.SYSTEM_ERROR, "无法获取用户角色，请确认 power-auth 已启动");
        }
    }

    /**
     * 解析用户展示名：优先昵称，否则用户名；失败时回退为 userId。
     *
     * @param userIdStr 用户 ID 字符串
     * @return 展示名
     */
    public String resolveDisplayName(String userIdStr) {
        if (!StringUtils.hasText(userIdStr)) {
            return null;
        }
        Long userId;
        try {
            userId = Long.valueOf(userIdStr.trim());
        } catch (NumberFormatException ex) {
            return userIdStr;
        }
        return displayNameCache.computeIfAbsent(userId, this::loadDisplayName);
    }

    private String loadDisplayName(Long userId) {
        try {
            R<WorkflowUserView> resp = authWorkflowIdentityClient.getUser(userId);
            if (resp == null || resp.getCode() != ErrorCode.SUCCESS.getCode() || resp.getData() == null) {
                return String.valueOf(userId);
            }
            WorkflowUserView user = resp.getData();
            if (StringUtils.hasText(user.getNickname())) {
                return user.getNickname();
            }
            if (StringUtils.hasText(user.getUsername())) {
                return user.getUsername();
            }
            return String.valueOf(userId);
        } catch (Exception ex) {
            log.warn("Load display name failed for userId={}: {}", userId, ex.getMessage());
            return String.valueOf(userId);
        }
    }
}
