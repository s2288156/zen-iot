package com.zen.ecs.protocol;

/**
 * 适配器需要的最小连接输入，取自设备档案。
 *
 * <p>刻意不引用持久化实体：协议层要能被单测直接构造，也不该被实体图拖进 JPA 会话。 {@code endpoint} 的语法由具体协议自己解释，ECS 只做透传。
 */
public record DeviceTarget(String deviceCode, String protocolType, String endpoint) {}
