package org.zeniot.data.domain.simulator.transport;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.zeniot.data.domain.transport.Transport;
import org.zeniot.data.enums.TransportTypeEnum;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Wu.Chunyang
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SimulatorMqttTransportConfig extends SimulatorTransportConfig implements Transport {

    private String saveTimeseriesTopic;

    private long period;

    private TimeUnit timeUnit;

    private List<FieldDefinition> timeseriesFields;

    private String host;

    private int port;

    @Override
    public TransportTypeEnum getType() {
        return TransportTypeEnum.MQTT;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public void setPort(int port) {
        this.port = port;
    }
}
