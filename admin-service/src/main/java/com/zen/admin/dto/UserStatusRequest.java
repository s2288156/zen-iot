package com.zen.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 启停用户入参。启用/禁用共用一个接口：状态机只有 0/1 两态，拆开只会多出两个语义相同的路由。 */
public record UserStatusRequest(
        @NotNull @Min(0) @Max(1) @Schema(description = "目标状态：1 启用 / 0 禁用", example = "0")
        Integer status) {}
