package com.power.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.power.auth.domain.SysUser;
import com.power.auth.mapper.SysUserMapper;
import com.power.common.model.PageQuery;
import com.power.common.model.PageResult;
import com.power.common.result.R;
import com.power.middleware.security.SecurityUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class UserController {

    private final SysUserMapper sysUserMapper;

    @GetMapping("/me")
    public R<MeResponse> me() {
        MeResponse resp = new MeResponse();
        resp.setUserId(SecurityUtils.currentUserId());
        resp.setUsername(SecurityUtils.currentUsername());
        if (SecurityUtils.currentUser() != null) {
            resp.setAuthorities(SecurityUtils.currentUser().getAuthorities());
        }
        return R.ok(resp);
    }

    @GetMapping("/users")
    @PreAuthorize("@authz.permit('system:user:list')")
    public R<PageResult<SysUser>> users(PageQuery query) {
        Page<SysUser> page = sysUserMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<SysUser>().orderByDesc(SysUser::getId));
        page.getRecords().forEach(u -> u.setPassword(null));
        return R.ok(PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @Data
    public static class MeResponse {
        private Long userId;
        private String username;
        private java.util.List<String> authorities;
    }
}
