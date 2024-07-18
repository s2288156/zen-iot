package org.zen.iot.api;

import org.zen.iot.data.base.PageQuery;
import org.zen.iot.data.base.PageResponse;
import org.zen.iot.data.domain.simulator.Simulator;
import org.zen.iot.data.domain.simulator.transport.SimulatorTransportConfig;
import org.zen.iot.data.enums.SimulatorStatusEnum;
import org.zen.iot.data.enums.TransportTypeEnum;

/**
 * @author Wu.Chunyang
 */
public interface SimulatorService {
    PageResponse<Simulator> findSimulators(PageQuery pageQuery);

    Simulator saveSimulator(Simulator simulator);

    void deleteSimulator(Long id);

    Simulator switchSimulatorPower(Long id, SimulatorStatusEnum toStatus);

    SimulatorTransportConfig defaultTransportConfig(TransportTypeEnum transportType);
}
