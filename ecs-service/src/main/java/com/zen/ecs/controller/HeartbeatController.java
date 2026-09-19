package com.zen.ecs.controller;

import com.zen.common.security.api.ApiResponse;
import com.zen.common.security.auth.ModuleCode;
import com.zen.common.security.auth.RequireModule;
import com.zen.ecs.service.HeartbeatService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 心跳上报入口。
 *
 * <p>按 {@code deviceCode} 而不是主键寻址：设备只知道自己的编码，让它持有并回传内部主键只会多一层映射错误的可能。
 *
 * <p>本阶段调用方是带 Token 的运营侧/联调脚本，因此与业务接口同样要求 {@link ModuleCode#ECS}。真正面向设备的上报通道
 * （Modbus 轮询、OPC UA 订阅）随 Phase 4 的协议适配器落地，那时才需要设备侧的身份与免鉴权白名单。
 */
@RestController
@RequestMapping("/heartbeat")
public class HeartbeatController {

    private final HeartbeatService heartbeatService;

    public HeartbeatController(HeartbeatService heartbeatService) {
        this.heartbeatService = heartbeatService;
    }

    @PostMapping("/{deviceCode}")
    @RequireModule(ModuleCode.ECS)
    public ApiResponse<Void> report(@PathVariable String deviceCode) {
        heartbeatService.report(deviceCode);
        return ApiResponse.success();
    }
}
