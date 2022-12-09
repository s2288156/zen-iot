package org.zeniot.server.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zeniot.api.SimulatorService;
import org.zeniot.data.base.SingleResponse;
import org.zeniot.data.domain.simulator.Simulator;

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

}
