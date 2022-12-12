package org.zeniot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.zeniot.api.SimulatorService;
import org.zeniot.dao.model.SimulatorEntity;
import org.zeniot.dao.repository.SimulatorRepository;
import org.zeniot.data.base.PageQuery;
import org.zeniot.data.base.PageResponse;
import org.zeniot.data.domain.simulator.Simulator;
import org.zeniot.service.mapper.SimulatorMapper;

import java.util.List;

/**
 * @author Wu.Chunyang
 */
@Service
public class SimulatorServiceImpl implements SimulatorService {

    @Autowired
    private SimulatorRepository simulatorRepository;
    @Autowired
    private SimulatorMapper simulatorMapper;

    @Override
    public PageResponse<Simulator> findSimulators(PageQuery pageQuery) {
        Page<SimulatorEntity> simulatorPage = simulatorRepository.findAll(pageQuery.toPageable());
        List<Simulator> simulators = simulatorPage.getContent().stream().map(simulatorMapper::entityToSimulator)
                .toList();
        return PageResponse.ok(simulators, simulatorPage);
    }

    @Override
    public Simulator saveSimulator(Simulator simulator) {
        SimulatorEntity save = simulatorRepository.save(simulatorMapper.toEntity(simulator));
        simulator.setId(save.getId());
        return simulator;
    }

    @Override
    public boolean deleteSimulator(Long id) {
        simulatorRepository.deleteById(id);
        return true;
    }
}
