package com.zen.ecs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 修改设备档案。设备编码是 MQ 指令定位设备的依据，不允许改，因此不在入参里。 */
public record DeviceUpdateRequest(
        @NotBlank @Size(max = 64) String deviceName,
        Long groupId,
        @NotBlank @Size(max = 32) String protocolType,
        @NotBlank @Size(max = 255) String endpoint) {}
