package com.zen.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 登出入参。
 *
 * <p>access Token 的撤销取自当前请求上下文,refresh Token 只能由客户端交出——它不在 access 的 claims 里,服务端无从得知。
 */
public record LogoutRequest(
        @NotBlank
        @Schema(
                description = "与当前 Authorization 里那枚 access Token 同一次登录发出的 refresh Token",
                example = "eyJhbGciOiJIUzI1NiJ9...")
        String refreshToken) {}
