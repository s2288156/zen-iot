package org.zeniot.server.transport;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.zeniot.transport.api.MqttTransportService;
import org.zeniot.transport.mqtt.MqttBrokerServer;

/**
 * @author Wu.Chunyang
 */
@Component
public class MqttBrokerServerInitializer {

    @Autowired
    private MqttTransportService mqttTransportService;

    MqttBrokerServer mqttBrokerServer;

    @PostConstruct
    public void init() {
        mqttBrokerServer = new MqttBrokerServer(mqttTransportService);
        mqttBrokerServer.init();
    }

    @PreDestroy
    public void shutdown() throws InterruptedException {
        mqttBrokerServer.shutdown();
    }
}
