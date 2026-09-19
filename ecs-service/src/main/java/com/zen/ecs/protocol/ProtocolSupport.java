package com.zen.ecs.protocol;

/**
 * 一种协议的实现入口：认得出自己负责的 {@code protocolType}，并能按目标地址开一个适配器。
 *
 * <p>这就是「可插拔」的那颗插槽：Phase 4 落地 Modbus TCP / OPC UA 时，各写一个实现类注册成 Spring Bean 即可被
 * {@link DeviceAdapterFactory} 发现，不需要改 ECS 的任何既有代码。
 */
public interface ProtocolSupport {

    boolean supports(String protocolType);

    DeviceAdapter open(DeviceTarget target);
}
