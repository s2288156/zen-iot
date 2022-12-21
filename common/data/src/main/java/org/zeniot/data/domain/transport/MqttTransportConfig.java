package org.zeniot.data.domain.transport;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.zeniot.data.enums.TransportTypeEnum;

import java.util.concurrent.TimeUnit;

/**
 * @author Wu.Chunyang
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MqttTransportConfig extends TransportConfig {

    private String saveTimeseriesTopic;

    private long period;

    private TimeUnit timeUnit;

    @Override
    public TransportTypeEnum getType() {
        return TransportTypeEnum.MQTT;
    }
}
