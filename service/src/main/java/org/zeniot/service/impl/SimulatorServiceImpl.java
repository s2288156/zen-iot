package org.zeniot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zeniot.api.SimulatorService;
import org.zeniot.common.exception.BizException;
import org.zeniot.dao.model.SimulatorEntity;
import org.zeniot.dao.repository.SimulatorRepository;
import org.zeniot.data.base.PageQuery;
import org.zeniot.data.base.PageResponse;
import org.zeniot.data.domain.simulator.Simulator;
import org.zeniot.data.enums.SimulatorStatusEnum;
import org.zeniot.service.mapper.SimulatorMapper;
import org.zeniot.transport.api.SimulatorManagement;

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
        return PageResponse.ok(simulators, simulatorPage);
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
    public boolean deleteSimulator(Long id) {
        simulatorRepository.deleteById(id);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Simulator switchSimulatorPower(Long id) {
        SimulatorEntity oldSimulator = simulatorRepository.findById(id)
                .orElseThrow(() -> new BizException("Simulator not exist."));

        // 更新设备状态，添加事务
        oldSimulator.setStatus(oldSimulator.getStatus().reverse());
        simulatorRepository.save(oldSimulator);
        if (oldSimulator.getStatus() == SimulatorStatusEnum.ENABLE) {
            simulatorManagement.enableSimulator(simulatorMapper.entityToSimulator(oldSimulator));
        } else {
            simulatorManagement.disableSimulator(simulatorMapper.entityToSimulator(oldSimulator));
        }
        return simulatorMapper.entityToSimulator(oldSimulator);
    }
}
