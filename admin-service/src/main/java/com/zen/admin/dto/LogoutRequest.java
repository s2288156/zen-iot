package com.zen.admin.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登出入参。
 *
 * <p>access Token 的撤销取自当前请求上下文,refresh Token 只能由客户端交出——它不在 access 的 claims 里,服务端无从得知。
 */
public record LogoutRequest(@NotBlank String refreshToken) {}
