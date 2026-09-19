package com.zen.ecs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 修改设备分组：分组编码是设备档案引用它的地方，不允许改。 */
public record DeviceGroupUpdateRequest(
        @NotBlank @Size(max = 64) String groupName,
        @Size(max = 255) String description) {}
