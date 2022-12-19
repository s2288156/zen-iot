package org.zeniot.data.domain.transport;

import org.zeniot.data.enums.TransportTypeEnum;

import java.util.concurrent.TimeUnit;

/**
 * @author Wu.Chunyang
 */
public class MqttTransportConfig implements TransportConfig {

    private String saveTimeseriesTopic;

    private long period;

    private TimeUnit timeUnit;

    @Override
    public TransportTypeEnum getType() {
        return TransportTypeEnum.MQTT;
    }
}
