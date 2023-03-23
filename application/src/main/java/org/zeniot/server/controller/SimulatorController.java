package org.zeniot.server.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.zeniot.api.SimulatorService;
import org.zeniot.data.base.PageQuery;
import org.zeniot.data.base.PageResponse;
import org.zeniot.data.base.SingleResponse;
import org.zeniot.data.domain.simulator.Simulator;
import org.zeniot.data.domain.simulator.transport.SimulatorTransportConfig;
import org.zeniot.data.enums.TransportTypeEnum;

/**
 * @author Wu.Chunyang
 */
@RequestMapping("/api")
@RestController
public class SimulatorController extends AbstractController {

    private final SimulatorService simulatorService;

    public SimulatorController(SimulatorService simulatorService) {
        this.simulatorService = simulatorService;
    }

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

    @PostMapping("/simulator/switch/{id}")
    public SingleResponse<Simulator> powerSwitch(@PathVariable Long id) {
        return SingleResponse.success(simulatorService.switchSimulatorPower(id));
    }

    @GetMapping("/simulator/defaultConfig")
    public SingleResponse<SimulatorTransportConfig> defaultTransportConfig(TransportTypeEnum transportType) {
        return SingleResponse.success(simulatorService.defaultTransportConfig(transportType));
    }

}
