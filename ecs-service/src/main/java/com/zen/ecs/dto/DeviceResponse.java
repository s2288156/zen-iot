package com.zen.ecs.dto;

import com.zen.ecs.entity.DeviceEntity;
import java.time.LocalDateTime;

/** 设备档案的对外契约：实体不出 service 层，入口层只看这个。 */
public record DeviceResponse(
        Long id,
        String deviceCode,
        String deviceName,
        Long groupId,
        String protocolType,
        String endpoint,
        boolean online,
        LocalDateTime lastHeartbeatTime) {

    public static DeviceResponse from(DeviceEntity device) {
        return new DeviceResponse(
                device.getId(),
                device.getDeviceCode(),
                device.getDeviceName(),
                device.getGroupId(),
                device.getProtocolType(),
                device.getEndpoint(),
                device.isOnline(),
                device.getLastHeartbeatTime());
    }
}
