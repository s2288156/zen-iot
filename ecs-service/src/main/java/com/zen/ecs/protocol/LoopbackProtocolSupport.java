package com.zen.ecs.protocol;

import org.springframework.stereotype.Component;

/** {@code loopback} 协议的注册入口。 */
@Component
public class LoopbackProtocolSupport implements ProtocolSupport {

    public static final String PROTOCOL_TYPE = "loopback";

    @Override
    public boolean supports(String protocolType) {
        return PROTOCOL_TYPE.equals(protocolType);
    }

    @Override
    public DeviceAdapter open(DeviceTarget target) {
        return new LoopbackDeviceAdapter(target.deviceCode(), target.endpoint());
    }
}
