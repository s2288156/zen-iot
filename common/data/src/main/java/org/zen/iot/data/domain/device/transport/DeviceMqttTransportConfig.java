package org.zen.iot.data.domain.device.transport;

import org.zen.iot.data.enums.TransportTypeEnum;

/**
 * @author Wu.Chunyang
 */
public class DeviceMqttTransportConfig extends DeviceTransportConfig {

    private String timeseriesTopic;

    @Override
    TransportTypeEnum getType() {
        return TransportTypeEnum.MQTT;
    }
}
