package com.zen.ecs.entity;

/** 指令执行状态，落库为 {@code t_device_command.status}。 */
public enum CommandStatus {
    PENDING,
    SUCCESS,
    FAILED
}
