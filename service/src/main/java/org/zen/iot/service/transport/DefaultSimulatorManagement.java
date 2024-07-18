package org.zen.iot.service.transport;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.zen.iot.common.exception.BizException;
import org.zen.iot.data.domain.simulator.Simulator;
import org.zen.iot.transport.api.SimulatorManagement;
import org.zen.iot.transport.mqtt.client.MqttClient;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Wu.Chunyang
 */
@Slf4j
@Component
public class DefaultSimulatorManagement implements SimulatorManagement {

    private final ConcurrentMap<Long, MqttClient> simulators = new ConcurrentHashMap<>();

    @Override
    public void enableSimulator(Simulator simulator) {
        MqttClient existClient = simulators.get(simulator.getId());
        if (existClient != null && existClient.isConnected()) {
            existClient.shutdown();
        }
        MqttClient mqttClient = new MqttClient(simulator);
        if (mqttClient.connect()) {
            simulators.put(simulator.getId(), mqttClient);
        } else {
            throw new BizException("Simulator mqtt client connect fail");
        }
    }

    @Override
    public void disableSimulator(Simulator simulator) {
        MqttClient existClient = simulators.get(simulator.getId());
        if (existClient == null) {
            throw new BizException("Simulator mqtt client not existed");
        }
        existClient.shutdown();
    }

}
