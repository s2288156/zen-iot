package com.zen.ecs.dto;

import com.zen.ecs.entity.CommandStatus;
import com.zen.ecs.entity.DeviceCommandEntity;
import java.time.LocalDateTime;

/** 一次指令中转的结果，供 RCS / 前端回读执行状态与重试次数。 */
public record CommandResponse(
        Long id,
        String commandNo,
        Long deviceId,
        String commandType,
        String payload,
        CommandStatus status,
        Integer retryCount,
        String errorMessage,
        LocalDateTime finishTime) {

    public static CommandResponse from(DeviceCommandEntity command) {
        return new CommandResponse(
                command.getId(),
                command.getCommandNo(),
                command.getDeviceId(),
                command.getCommandType(),
                command.getPayload(),
                command.getStatus(),
                command.getRetryCount(),
                command.getErrorMessage(),
                command.getFinishTime());
    }
}
