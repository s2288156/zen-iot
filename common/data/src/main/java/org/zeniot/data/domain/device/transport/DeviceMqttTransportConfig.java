package org.zeniot.data.domain.device.transport;

import org.zeniot.data.enums.TransportTypeEnum;

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
