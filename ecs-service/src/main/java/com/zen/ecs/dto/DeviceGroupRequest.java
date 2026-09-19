package com.zen.ecs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 新建设备分组。 */
public record DeviceGroupRequest(
        @NotBlank @Size(max = 64) String groupCode,
        @NotBlank @Size(max = 64) String groupName,
        @Size(max = 255) String description) {}
