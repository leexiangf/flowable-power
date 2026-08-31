package com.power.auth.controller;

import com.power.auth.dto.LoginRequest;
import com.power.auth.dto.LoginResponse;
import com.power.auth.dto.RefreshRequest;
import com.power.auth.service.AuthService;
import com.power.common.result.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return R.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public R<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return R.ok(authService.refresh(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public R<Void> logout(@RequestBody(required = false) RefreshRequest request) {
        if (request != null) {
            authService.logout(request.getRefreshToken());
        }
        return R.ok();
    }
}
