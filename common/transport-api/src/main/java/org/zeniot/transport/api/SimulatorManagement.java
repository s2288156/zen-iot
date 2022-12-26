package org.zeniot.transport.api;

import org.zeniot.data.domain.simulator.Simulator;

/**
 * @author Wu.Chunyang
 */
public interface SimulatorManagement {
    boolean enableSimulator(Simulator simulator);

    boolean disableSimulator(Simulator simulator);
}
