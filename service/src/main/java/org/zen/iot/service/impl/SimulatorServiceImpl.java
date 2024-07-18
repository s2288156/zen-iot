package org.zen.iot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zen.iot.api.SimulatorService;
import org.zen.iot.common.exception.BizException;
import org.zen.iot.dao.model.SimulatorEntity;
import org.zen.iot.dao.repository.SimulatorRepository;
import org.zen.iot.data.base.PageQuery;
import org.zen.iot.data.base.PageResponse;
import org.zen.iot.data.domain.simulator.Simulator;
import org.zen.iot.data.domain.simulator.transport.SimulatorMqttTransportConfig;
import org.zen.iot.data.domain.simulator.transport.SimulatorTransportConfig;
import org.zen.iot.data.enums.SimulatorStatusEnum;
import org.zen.iot.data.enums.TransportTypeEnum;
import org.zen.iot.service.mapper.SimulatorMapper;
import org.zen.iot.transport.api.SimulatorManagement;

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
    @Autowired
    private SimulatorManagement simulatorManagement;

    @Override
    public PageResponse<Simulator> findSimulators(PageQuery pageQuery) {
        Page<SimulatorEntity> simulatorPage = simulatorRepository.findAll(pageQuery.toPageable());
        List<Simulator> simulators = simulatorPage.getContent().stream().map(simulatorMapper::entityToSimulator)
                .toList();
        return PageResponse.success(simulators, simulatorPage);
    }

    @Override
    public Simulator saveSimulator(Simulator simulator) {
        if (simulator.getId() == null) {
            simulator.setStatus(SimulatorStatusEnum.DISABLE);
        }
        SimulatorEntity save = simulatorRepository.save(simulatorMapper.toEntity(simulator));
        simulator.setId(save.getId());
        return simulator;
    }

    @Override
    public void deleteSimulator(Long id) {
        simulatorRepository.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Simulator switchSimulatorPower(Long id, SimulatorStatusEnum toStatus) {
        SimulatorEntity oldSimulator = simulatorRepository.findById(id)
                .orElseThrow(() -> new BizException("Simulator not exist."));
        if (oldSimulator.getStatus() != toStatus) {
            oldSimulator.setStatus(toStatus);
            simulatorRepository.save(oldSimulator);
            if (oldSimulator.getStatus() == SimulatorStatusEnum.ENABLE) {
                simulatorManagement.enableSimulator(simulatorMapper.entityToSimulator(oldSimulator));
            } else {
                simulatorManagement.disableSimulator(simulatorMapper.entityToSimulator(oldSimulator));
            }
        }
        return simulatorMapper.entityToSimulator(oldSimulator);
    }

    @Override
    public SimulatorTransportConfig defaultTransportConfig(TransportTypeEnum transportType) {
        SimulatorTransportConfig config = null;
        switch (transportType) {
            case MQTT -> config = SimulatorMqttTransportConfig.newDefault();
        }
        return config;
    }
}
