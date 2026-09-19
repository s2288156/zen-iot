package com.zen.ecs.protocol;

/**
 * 一次指令写入的结果。
 *
 * <p>{@code accepted=false} 表示设备明确拒绝，属于不可重试的业务失败；只有连接异常、读写字节超时才会走
 * {@code com.zen.ecs.service.CommandDispatchService} 的重试。
 */
public record CommandOutcome(boolean accepted, String detail) {

    public static CommandOutcome accepted(String detail) {
        return new CommandOutcome(true, detail);
    }

    public static CommandOutcome rejected(String detail) {
        return new CommandOutcome(false, detail);
    }
}
