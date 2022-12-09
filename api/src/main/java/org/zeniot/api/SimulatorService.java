package org.zeniot.api;

import org.zeniot.data.base.PageQuery;
import org.zeniot.data.base.PageResponse;
import org.zeniot.data.domain.simulator.Simulator;

/**
 * @author Wu.Chunyang
 */
public interface SimulatorService {
    PageResponse<Simulator> findSimulators(PageQuery pageQuery);

    Simulator saveSimulator(Simulator simulator);

    boolean deleteSimulator(Long id);
}
