package org.zen.iot.service.transport;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.zen.iot.transport.api.MqttBrokerService;
import org.zen.iot.transport.mqtt.broker.MqttBrokerServer;

/**
 * @author Wu.Chunyang
 */
@Component
public class MqttBrokerServerInitializer {

    @Autowired
    private MqttBrokerService mqttBrokerService;

    MqttBrokerServer mqttBrokerServer;

    @PostConstruct
    public void init() {
        mqttBrokerServer = new MqttBrokerServer(mqttBrokerService);
        mqttBrokerServer.init();
    }

    @PreDestroy
    public void shutdown() throws InterruptedException {
        mqttBrokerServer.shutdown();
    }
}
