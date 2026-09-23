package com.zen.admin.controller;

import com.zen.admin.config.ZenAdminOpenApiConfiguration;
import com.zen.admin.dto.LoginRequest;
import com.zen.admin.dto.LogoutRequest;
import com.zen.admin.dto.RefreshRequest;
import com.zen.admin.service.AuthService;
import com.zen.common.security.api.ApiResponse;
import com.zen.common.security.jwt.TokenPair;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录与登出。
 *
 * <p>路径不带 {@code /api}：{@code /api/admin} 前缀只存在于网关侧，{@code StripPrefix=2} 剥掉后才进本服务。
 *
 * <p>只标注 {@link Operation} 与 {@link SecurityRequirement}：状态码 400/401/403 由
 * {@link ZenAdminOpenApiConfiguration} 按请求体、鉴权要求与 {@code @RequireModule} 统一补齐——在方法上写
 * {@code @ApiResponses} 会让 springdoc 不再自动生成成功响应，连带 {@code ApiResponse*} 模型一起从文档消失。
 */
@Tag(name = "认证", description = "登录、刷新与登出；只有 login/refresh 是免鉴权入口，logout 必须带身份。")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "账号密码登录，签发双 Token", description = "免鉴权入口。用户不存在与密码错误返回同一个 401 响应体，不透露账号是否存在。")
    @PostMapping("/login")
    public ApiResponse<TokenPair> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @Operation(
            summary = "用 refresh Token 换新双 Token",
            description = "免鉴权入口，但 Token 本身要过校验。轮转语义：旧 refresh Token 的 jti 当场进黑名单，重放即 401。")
    @PostMapping("/refresh")
    public ApiResponse<TokenPair> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.success(authService.refresh(request.refreshToken()));
    }

    @Operation(summary = "登出", description = "同时撤销本次请求携带的 access Token 与入参里的 refresh Token——只撤一个的话，登出后仍能换新 Token。")
    @SecurityRequirement(name = ZenAdminOpenApiConfiguration.BEARER_JWT_SCHEME)
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
        return ApiResponse.success();
    }
}
