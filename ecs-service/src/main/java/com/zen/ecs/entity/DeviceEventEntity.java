package com.zen.ecs.entity;

import com.zen.common.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 设备上下线事件。
 *
 * <p>{@code t_device.online} 只有当前态，这张表回答「谁在何时上下线、因何离线」，也是 Phase 4 事件联动的输入。 不带 {@code deleted}：事件是事实记录，不做
 * CRUD。
 */
@Getter
@Setter
@Entity
@Table(name = "t_device_event")
public class DeviceEventEntity extends BaseEntity {

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 16)
    private DeviceEventType eventType;

    /** 事件时刻，由 service 侧注入的 {@code java.time.Clock} 算出，不取数据库时钟。 */
    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "reason", length = 255)
    private String reason;

    public static DeviceEventEntity of(
            long deviceId, DeviceEventType eventType, LocalDateTime occurredAt, String reason) {
        DeviceEventEntity event = new DeviceEventEntity();
        event.deviceId = deviceId;
        event.eventType = eventType;
        event.occurredAt = occurredAt;
        event.reason = reason;
        return event;
    }
}
