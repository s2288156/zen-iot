package org.zeniot.data.domain.simulator.transport;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.zeniot.data.domain.transport.Transport;
import org.zeniot.data.enums.FieldTypeEnum;
import org.zeniot.data.enums.TransportTypeEnum;

import java.util.Collections;
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

    public static SimulatorMqttTransportConfig newDefault() {
        FieldDefinition fieldDefinition = new FieldDefinition();
        fieldDefinition.setFieldType(FieldTypeEnum.INTEGER);
        fieldDefinition.setName("temperature");
        fieldDefinition.setValueOrigin("1");
        fieldDefinition.setValueBound("100");
        SimulatorMqttTransportConfig config = new SimulatorMqttTransportConfig();
        config.setSaveTimeseriesTopic("save/timeseries");
        config.setPeriod(1);
        config.setTimeUnit(TimeUnit.SECONDS);
        config.setTimeseriesFields(Collections.singletonList(fieldDefinition));
        config.setHost("127.0.0.1");
        config.setPort(1883);
        return config;
    }
}
