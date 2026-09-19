package com.zen.ecs.controller;

import com.zen.common.security.api.ApiResponse;
import com.zen.common.security.auth.ModuleCode;
import com.zen.common.security.auth.RequireModule;
import com.zen.ecs.dto.CommandResponse;
import com.zen.ecs.service.CommandDispatchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 指令中转结果回读。
 *
 * <p>只有 GET：指令的唯一入口是 RCS 经 MQ 的下发，这里再开一个 POST 就等于给同一条指令两条语义不同的路径
 * （HTTP 那条还没有幂等键的来源），排查问题时应看 {@code t_device_command} 的落库结果而不是重放。
 */
@RestController
@RequestMapping("/command")
public class CommandController {

    private final CommandDispatchService commandDispatchService;

    public CommandController(CommandDispatchService commandDispatchService) {
        this.commandDispatchService = commandDispatchService;
    }

    @GetMapping("/{commandNo}")
    @RequireModule(ModuleCode.ECS)
    public ApiResponse<CommandResponse> get(@PathVariable String commandNo) {
        return ApiResponse.success(commandDispatchService.get(commandNo));
    }
}
