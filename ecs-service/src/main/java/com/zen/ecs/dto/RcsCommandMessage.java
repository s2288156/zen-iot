package com.zen.ecs.dto;

/**
 * RCS → ECS 的指令消息（JSON）。
 *
 * <p>{@code commandNo} 由 RCS 生成且全局唯一，是消费侧的幂等键；MQ 至少一次投递，缺了它就没办法证明「重复投递不执行第二次」。
 * Exchange / Queue / Routing Key 的完整拓扑由 Phase 9 统一定案，本阶段只落 ECS 这一侧的消费契约。
 *
 * <p>不带 Bean Validation 注解：这条链路的入口是 MQ 而非 HTTP，校验失败没有响应可回。字段缺失/非法由
 * {@code com.zen.ecs.service.CommandDispatchService} 判成不可重试的失败并留痕。
 */
public record RcsCommandMessage(String commandNo, String deviceCode, String commandType, String payload) {}
