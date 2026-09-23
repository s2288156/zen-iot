package com.zen.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** 用 refresh Token 换新双 Token。 */
public record RefreshRequest(
        @NotBlank
        @Schema(
                description = "登录或上一次刷新响应里的 data.refreshToken（不是 accessToken，两者不可互换）",
                example = "eyJhbGciOiJIUzI1NiJ9...")
        String refreshToken) {}
