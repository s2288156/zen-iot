package org.zeniot.server.transport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.zeniot.data.domain.simulator.Simulator;
import org.zeniot.transport.api.MqttBrokerService;
import org.zeniot.transport.api.SimulatorManagement;

/**
 * @author Wu.Chunyang
 */
@Component
public class DefaultSimulatorManagement implements SimulatorManagement {
    @Autowired
    private MqttBrokerService mqttBrokerService;

    @Override
    public boolean enableSimulator(Simulator simulator) {
        return false;
    }

    @Override
    public boolean disableSimulator(Simulator simulator) {
        return false;
    }
}
