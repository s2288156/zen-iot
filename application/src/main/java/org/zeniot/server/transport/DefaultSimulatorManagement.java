package org.zeniot.server.transport;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.zeniot.data.domain.simulator.Simulator;
import org.zeniot.transport.api.MqttBrokerService;
import org.zeniot.transport.api.SimulatorManagement;
import org.zeniot.transport.mqtt.MqttClient;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Wu.Chunyang
 */
@Slf4j
@Component
public class DefaultSimulatorManagement implements SimulatorManagement {
    @Autowired
    private MqttBrokerService mqttBrokerService;

    private final ConcurrentMap<Long, MqttClient> simulators = new ConcurrentHashMap();

    @Override
    public boolean enableSimulator(Simulator simulator) {
        MqttClient existClient = simulators.get(simulator.getId());
        if (existClient != null) {
            existClient.shutdown();
        }
        MqttClient mqttClient = new MqttClient(simulator);
        if (mqttClient.connect()) {
            simulators.put(simulator.getId(), mqttClient);
            return true;
        }
        return false;
    }

    public static void main(String[] args) {
        ConcurrentMap<String, Integer> map = new ConcurrentHashMap<>();
        log.warn("{}", map.put("a", 1));
        log.warn("{}", map.put("a", 2));
        log.warn("{}", map.put("a", 4));
        log.warn("{}", map.put("a", 8));
    }

    @Override
    public boolean disableSimulator(Simulator simulator) {

        return false;
    }
}
