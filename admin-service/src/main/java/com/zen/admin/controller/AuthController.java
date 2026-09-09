package com.zen.admin.controller;

import com.zen.admin.dto.LoginRequest;
import com.zen.admin.dto.LogoutRequest;
import com.zen.admin.dto.RefreshRequest;
import com.zen.admin.service.AuthService;
import com.zen.common.core.api.ApiResponse;
import com.zen.common.core.jwt.TokenPair;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录与登出。
 *
 * <p>路径不带 {@code /api}：{@code /api/admin} 前缀只存在于网关侧，{@code StripPrefix=2} 剥掉后才进本服务。
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<TokenPair> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenPair> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.success(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
        return ApiResponse.success();
    }
}
