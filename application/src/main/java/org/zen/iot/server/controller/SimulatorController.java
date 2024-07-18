package org.zen.iot.server.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.zen.iot.data.base.PageQuery;
import org.zen.iot.data.base.PageResponse;
import org.zen.iot.data.base.SingleResponse;
import org.zen.iot.data.command.SimulatorSwitchPowerCmd;
import org.zen.iot.data.domain.simulator.Simulator;
import org.zen.iot.data.domain.simulator.transport.SimulatorTransportConfig;
import org.zen.iot.data.enums.TransportTypeEnum;

/**
 * @author Wu.Chunyang
 */
@RequestMapping("/api")
@RestController
public class SimulatorController extends AbstractController {

    @PostMapping("/simulator")
    public SingleResponse<Simulator> saveSimulator(@RequestBody @Validated Simulator simulator) {
        return SingleResponse.success(simulatorService.saveSimulator(simulator));
    }

    @GetMapping("/simulators")
    public PageResponse<Simulator> getSimulators(@Validated PageQuery pageQuery) {
        return simulatorService.findSimulators(pageQuery);
    }

    @DeleteMapping("/simulator/{id}")
    public SingleResponse<Simulator> deleteSimulator(@PathVariable Long id) {
        simulatorService.deleteSimulator(id);
        return SingleResponse.success();
    }

    @PostMapping("/simulator/switch")
    public SingleResponse<Simulator> simulatorPowerSwitch(@Validated SimulatorSwitchPowerCmd cmd) {
        return SingleResponse.success(simulatorService.switchSimulatorPower(cmd.getId(), cmd.getStatus()));
    }

    @GetMapping("/simulator/defaultConfig")
    public SingleResponse<SimulatorTransportConfig> defaultTransportConfig(TransportTypeEnum transportType) {
        return SingleResponse.success(simulatorService.defaultTransportConfig(transportType));
    }

}
