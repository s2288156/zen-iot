package com.zen.admin.controller;

import com.zen.admin.config.ZenAdminOpenApiConfiguration;
import com.zen.admin.dto.ChangePasswordRequest;
import com.zen.admin.dto.LoginRequest;
import com.zen.admin.dto.LogoutRequest;
import com.zen.admin.dto.RefreshRequest;
import com.zen.admin.dto.UserProfileView;
import com.zen.admin.service.AuthService;
import com.zen.common.security.api.ApiResponse;
import com.zen.common.security.jwt.TokenPair;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
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
@Tag(name = "认证", description = "登录、刷新、登出与个人中心；只有 login/refresh 是免鉴权入口，其余必须带身份。")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "账号密码登录，签发双 Token",
            description = "免鉴权入口。用户不存在与密码错误返回同一个 401 响应体，不透露账号是否存在。"
                    + "窗口内连续失败达阈值（默认 5 次）触发锁定：达阈值那次与锁定期内的尝试一律返回 429，锁定期不计数不续期，到期自动解锁。")
    @PostMapping("/login")
    public ApiResponse<TokenPair> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.success(
                authService.login(request, clientIp(httpRequest), httpRequest.getHeader("User-Agent")));
    }

    /**
     * 真实客户端 IP：取 {@code X-Forwarded-For} 最后一个值——网关 {@code JwtAuthGlobalFilter} 已剥离入站伪造
     * 并以连接对端重建（PR #30），最后一个值即网关认定的客户端。无 XFF 说明请求不经网关（本机直连/运维 curl），
     * 回落 {@code getRemoteAddr()}，记的是连接对端而非浏览器（见 README 已知坑）。
     */
    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.lastIndexOf(',');
            String last = (comma >= 0 ? forwarded.substring(comma + 1) : forwarded).trim();
            if (!last.isEmpty()) {
                return last;
            }
        }
        return request.getRemoteAddr();
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

    @Operation(
            summary = "当前登录用户资料",
            description = "只要求登录，无模块权限要求。按身份回查库内最新资料（角色只回 roleIds），不含 modules 与审计列；用户已被逻辑删时返回 401。")
    @SecurityRequirement(name = ZenAdminOpenApiConfiguration.BEARER_JWT_SCHEME)
    @GetMapping("/me")
    public ApiResponse<UserProfileView> me() {
        return ApiResponse.success(authService.me());
    }

    @Operation(
            summary = "自助改密",
            description = "只要求登录。旧口令 BCrypt 比对，不符返回 400。改密成功连带吊销该用户除当前会话外的全部会话："
                    + "旧谱系在新口令生效后不能再续期，调用人不会被自己的改密踢下线。吊销失败则整个改密回滚。")
    @SecurityRequirement(name = ZenAdminOpenApiConfiguration.BEARER_JWT_SCHEME)
    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ApiResponse.success();
    }
}
