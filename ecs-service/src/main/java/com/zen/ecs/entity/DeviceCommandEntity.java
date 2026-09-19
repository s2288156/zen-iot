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
 * RCS 下发指令的中转记录，一行对应一次「消费到执行完毕」。
 *
 * <p>{@code commandNo} 的唯一约束就是幂等键：MQ 至少一次投递，重复消息必须不再碰设备第二次。 设备编码查无此设备时 {@code deviceId} 为空，只留痕不执行。
 */
@Getter
@Setter
@Entity
@Table(name = "t_device_command")
public class DeviceCommandEntity extends BaseEntity {

    @Column(name = "command_no", nullable = false, length = 64, unique = true)
    private String commandNo;

    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "command_type", nullable = false, length = 32)
    private String commandType;

    @Column(name = "payload", length = 1024)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private CommandStatus status;

    /** 已重试次数，不含首次执行；达到配置上限仍失败即为 FAILED。 */
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "error_message", length = 255)
    private String errorMessage;

    @Column(name = "finish_time")
    private LocalDateTime finishTime;

    public static DeviceCommandEntity pending(String commandNo, Long deviceId, String commandType, String payload) {
        DeviceCommandEntity command = new DeviceCommandEntity();
        command.commandNo = commandNo;
        command.deviceId = deviceId;
        command.commandType = commandType;
        command.payload = payload;
        command.status = CommandStatus.PENDING;
        command.retryCount = 0;
        return command;
    }

    public void finish(CommandStatus status, int retries, String errorMessage, LocalDateTime finishTime) {
        this.status = status;
        this.retryCount = retries;
        this.errorMessage = errorMessage;
        this.finishTime = finishTime;
    }
}
