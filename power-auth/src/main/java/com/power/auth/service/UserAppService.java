package com.power.auth.service;

import com.power.auth.domain.SysUser;
import com.power.auth.dto.CurrentUserVO;
import com.power.auth.dto.PasswordChangeRequest;
import com.power.auth.dto.ProfileUpdateRequest;
import com.power.auth.mapper.SysUserMapper;
import com.power.common.constant.ErrorCode;
import com.power.common.exception.BizException;
import com.power.middleware.security.SecurityUtils;
import com.power.middleware.security.TokenSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * 当前用户资料查询与自助修改。
 */
@Service
@RequiredArgsConstructor
public class UserAppService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenSessionService tokenSessionService;

    /**
     * 当前登录用户完整资料（含角色与权限码，权限从库实时加载）。
     */
    public CurrentUserVO currentProfile() {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return toCurrentUserVo(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public CurrentUserVO updateProfile(ProfileUpdateRequest request) {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (request.getNickname() != null) {
            user.setNickname(trimToNull(request.getNickname()));
        }
        if (request.getEmail() != null) {
            user.setEmail(trimToNull(request.getEmail()));
        }
        if (request.getPhone() != null) {
            user.setPhone(trimToNull(request.getPhone()));
        }
        if (request.getAvatar() != null) {
            user.setAvatar(trimToNull(request.getAvatar()));
        }
        sysUserMapper.updateById(user);
        return toCurrentUserVo(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void changePassword(PasswordChangeRequest request) {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "原密码不正确");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        sysUserMapper.updateById(user);
        tokenSessionService.bumpUserVersion(userId);
    }

    private CurrentUserVO toCurrentUserVo(SysUser user) {
        Long userId = user.getId();
        CurrentUserVO resp = new CurrentUserVO();
        resp.setUserId(userId);
        resp.setUsername(user.getUsername());
        resp.setNickname(user.getNickname());
        resp.setEmail(user.getEmail());
        resp.setPhone(user.getPhone());
        resp.setAvatar(user.getAvatar());
        resp.setStatus(user.getStatus());

        List<String> roles = sysUserMapper.selectRoleCodesByUserId(userId);
        resp.setRoles(roles == null ? Collections.emptyList() : roles);

        List<String> perms = sysUserMapper.selectPermsByUserId(userId);
        resp.setAuthorities(perms == null ? Collections.emptyList() : perms);
        return resp;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
