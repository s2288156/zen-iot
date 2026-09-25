package com.zen.admin.controller;

import com.zen.admin.config.ZenAdminOpenApiConfiguration;
import com.zen.admin.dto.SessionView;
import com.zen.admin.interceptor.OperationAction;
import com.zen.admin.interceptor.OperationLog;
import com.zen.admin.interceptor.OperationTargetType;
import com.zen.admin.service.SessionService;
import com.zen.common.core.page.PageQuery;
import com.zen.common.core.page.PageResult;
import com.zen.common.security.api.ApiResponse;
import com.zen.common.security.auth.ModuleCode;
import com.zen.common.security.auth.RequireModule;
import com.zen.common.security.auth.UserContext;
import com.zen.common.security.auth.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 在线会话：列表 + 强制下线（G5-2）。会话由登录建立、随 refresh 轮转迁移、登出自删，这里只做观测与吊销。
 *
 * <p>错误语义：目标会话不存在或已下线 404；踢自己的会话 400（防自锁）；两个接口都要求 {@code admin} 模块。
 * 响应刻意不暴露 jti——列表拿到的标识只用于下线路由，令牌本身不应经接口外泄。
 */
@Tag(name = "在线会话", description = "在线会话列表与强制下线；吊销成对拉黑 refresh/access jti，失败上抛而非静默。")
@RestController
@RequestMapping("/sessions")
@SecurityRequirement(name = ZenAdminOpenApiConfiguration.BEARER_JWT_SCHEME)
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Operation(summary = "在线会话列表", description = "按过期时刻升序分页；只反映尚未到 refresh 有效期的登记，过期条目读取时惰性剔除。")
    @GetMapping
    @RequireModule(ModuleCode.ADMIN)
    public ApiResponse<PageResult<SessionView>> list(@ParameterObject @Valid PageQuery query) {
        return ApiResponse.success(sessionService.page(query));
    }

    @Operation(summary = "强制下线", description = "成对拉黑会话谱系的 refresh/access jti 并删登记；目标属主是调用人自己返回 400，会话不存在返回 404。")
    @DeleteMapping("/{sessionId}")
    @RequireModule(ModuleCode.ADMIN)
    @OperationLog(
            action = OperationAction.DELETE,
            targetType = OperationTargetType.SESSION,
            targetIdParam = "sessionId")
    public ApiResponse<Void> kickout(@PathVariable String sessionId) {
        UserPrincipal current = UserContext.get();
        // AuthInterceptor 已在路由前保证身份存在，这里只是编译期非空衔接
        long callerUserId = current == null ? 0L : current.userId();
        sessionService.kickout(sessionId, callerUserId);
        return ApiResponse.success();
    }
}
