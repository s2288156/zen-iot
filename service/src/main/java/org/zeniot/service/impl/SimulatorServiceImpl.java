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
import org.zeniot.data.command.SimulatorSwitchPowerCmd;
import org.zeniot.data.domain.simulator.Simulator;
import org.zeniot.data.enums.SimulatorStatusEnum;
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
    public Simulator switchSimulatorPower(SimulatorSwitchPowerCmd powerCmd) {
        SimulatorEntity oldSimulator = simulatorRepository.findById(powerCmd.getId())
                .orElseThrow(() -> new BizException("Simulator not exist."));
        if (oldSimulator.getStatus() == powerCmd.getSimulatorStatus()) {
            return simulatorMapper.entityToSimulator(oldSimulator);
        }
        // 更新设备状态，添加事务
        oldSimulator.setStatus(powerCmd.getSimulatorStatus());
        simulatorRepository.save(oldSimulator);
        // TODO: 12/26/2022 关闭或启用设备

        return simulatorMapper.entityToSimulator(oldSimulator);
    }
}
