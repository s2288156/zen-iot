package org.zen.iot.transport.api;

import org.zen.iot.data.domain.simulator.Simulator;

/**
 * @author Wu.Chunyang
 */
public interface SimulatorManagement {
    void enableSimulator(Simulator simulator);

    void disableSimulator(Simulator simulator);
}
