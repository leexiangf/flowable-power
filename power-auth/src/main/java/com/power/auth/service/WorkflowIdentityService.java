package com.power.auth.service;

import com.power.auth.domain.SysUser;
import com.power.auth.dto.WorkflowUserVO;
import com.power.auth.mapper.SysUserMapper;
import com.power.common.constant.ErrorCode;
import com.power.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 工作流身份查询：对接 Flowable assignee / candidateGroups。
 */
@Service
@RequiredArgsConstructor
public class WorkflowIdentityService {

    private final SysUserMapper sysUserMapper;

    /**
     * 按用户 ID 查询工作流用户视图。
     *
     * @param userId 用户 ID
     * @return 用户视图（无密码）
     */
    public WorkflowUserVO getUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return toVo(user);
    }

    /**
     * 查询用户启用中的角色编码列表（对应 candidateGroups）。
     *
     * @param userId 用户 ID
     * @return roleCode 列表
     */
    public List<String> listRoleCodes(Long userId) {
        return sysUserMapper.selectRoleCodesByUserId(userId);
    }

    /**
     * 按角色编码查询启用用户 ID 列表。
     *
     * @param roleCode 角色编码
     * @return 用户 ID 列表
     */
    public List<Long> listUserIdsByRoleCode(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "roleCode 不能为空");
        }
        return sysUserMapper.selectUserIdsByRoleCode(roleCode.trim());
    }

    /**
     * 按角色编码查询用户详情列表。
     *
     * @param roleCode 角色编码
     * @return 用户视图列表
     */
    public List<WorkflowUserVO> listUsersByRoleCode(String roleCode) {
        return listUserIdsByRoleCode(roleCode).stream()
                .map(sysUserMapper::selectById)
                .filter(u -> u != null)
                .map(this::toVo)
                .collect(Collectors.toList());
    }

    /**
     * 用户实体转工作流视图。
     *
     * @param user 用户实体
     * @return 工作流用户视图
     */
    private WorkflowUserVO toVo(SysUser user) {
        return WorkflowUserVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .status(user.getStatus())
                .build();
    }
}
