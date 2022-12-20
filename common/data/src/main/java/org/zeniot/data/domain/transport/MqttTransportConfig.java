package org.zeniot.data.domain.transport;

import lombok.Data;
import org.zeniot.data.enums.TransportTypeEnum;

import java.util.concurrent.TimeUnit;

/**
 * @author Wu.Chunyang
 */
@Data
public class MqttTransportConfig implements TransportConfig {

    private String saveTimeseriesTopic;

    private long period;

    private TimeUnit timeUnit;

    @Override
    public TransportTypeEnum getType() {
        return TransportTypeEnum.MQTT;
    }
}
