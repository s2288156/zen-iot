package com.zen.ecs.controller;

import com.zen.common.core.page.PageQuery;
import com.zen.common.core.page.PageResult;
import com.zen.common.security.api.ApiResponse;
import com.zen.common.security.auth.ModuleCode;
import com.zen.common.security.auth.RequireModule;
import com.zen.ecs.dto.DeviceCreateRequest;
import com.zen.ecs.dto.DeviceEventResponse;
import com.zen.ecs.dto.DeviceResponse;
import com.zen.ecs.dto.DeviceUpdateRequest;
import com.zen.ecs.service.DeviceService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 设备档案。
 *
 * <p>路径不带 {@code /api}：{@code /api/ecs} 前缀只存在于网关侧，{@code StripPrefix=2} 剥掉后才进本服务。
 * 心跳上报不在这里——它按设备编码寻址（设备不知道我们的主键），见 {@code HeartbeatController}。
 */
@RestController
@RequestMapping("/device")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    @RequireModule(ModuleCode.ECS)
    public ApiResponse<PageResult<DeviceResponse>> page(
            @Valid @ModelAttribute PageQuery pageQuery, @RequestParam(required = false) Long groupId) {
        return ApiResponse.success(deviceService.page(pageQuery, groupId));
    }

    @GetMapping("/{deviceId}")
    @RequireModule(ModuleCode.ECS)
    public ApiResponse<DeviceResponse> get(@PathVariable long deviceId) {
        return ApiResponse.success(deviceService.get(deviceId));
    }

    @PostMapping
    @RequireModule(ModuleCode.ECS)
    public ApiResponse<DeviceResponse> create(@Valid @RequestBody DeviceCreateRequest request) {
        return ApiResponse.success(deviceService.create(request));
    }

    @PutMapping("/{deviceId}")
    @RequireModule(ModuleCode.ECS)
    public ApiResponse<DeviceResponse> update(
            @PathVariable long deviceId, @Valid @RequestBody DeviceUpdateRequest request) {
        return ApiResponse.success(deviceService.update(deviceId, request));
    }

    @DeleteMapping("/{deviceId}")
    @RequireModule(ModuleCode.ECS)
    public ApiResponse<Void> delete(@PathVariable long deviceId) {
        deviceService.delete(deviceId);
        return ApiResponse.success();
    }

    @GetMapping("/{deviceId}/events")
    @RequireModule(ModuleCode.ECS)
    public ApiResponse<List<DeviceEventResponse>> events(@PathVariable long deviceId) {
        return ApiResponse.success(deviceService.recentEvents(deviceId));
    }
}
