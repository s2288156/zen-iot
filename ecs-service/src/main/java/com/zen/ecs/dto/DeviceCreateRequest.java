package com.zen.ecs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 新建设备档案。分组可空（未分组），协议类型与地址由 Phase 4 的具体适配器解释。 */
public record DeviceCreateRequest(
        @NotBlank @Size(max = 64) String deviceCode,
        @NotBlank @Size(max = 64) String deviceName,
        Long groupId,
        @NotBlank @Size(max = 32) String protocolType,
        @NotBlank @Size(max = 255) String endpoint) {}
