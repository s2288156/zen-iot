package org.zeniot.transport.api;

import org.zeniot.data.domain.simulator.Simulator;

/**
 * @author Wu.Chunyang
 */
public interface SimulatorManagement {
    void enableSimulator(Simulator simulator);

    void disableSimulator(Simulator simulator);
}
