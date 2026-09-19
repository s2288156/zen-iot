package com.zen.ecs.dto;

import com.zen.ecs.entity.DeviceEventEntity;
import com.zen.ecs.entity.DeviceEventType;
import java.time.LocalDateTime;

/** 一条上下线事件。 */
public record DeviceEventResponse(
        Long id, Long deviceId, DeviceEventType eventType, LocalDateTime occurredAt, String reason) {

    public static DeviceEventResponse from(DeviceEventEntity event) {
        return new DeviceEventResponse(
                event.getId(), event.getDeviceId(), event.getEventType(), event.getOccurredAt(), event.getReason());
    }
}
