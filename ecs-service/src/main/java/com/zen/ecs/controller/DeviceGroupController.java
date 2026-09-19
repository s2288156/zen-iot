package com.zen.ecs.controller;

import com.zen.common.security.api.ApiResponse;
import com.zen.common.security.auth.ModuleCode;
import com.zen.common.security.auth.RequireModule;
import com.zen.ecs.dto.DeviceGroupRequest;
import com.zen.ecs.dto.DeviceGroupResponse;
import com.zen.ecs.dto.DeviceGroupUpdateRequest;
import com.zen.ecs.service.DeviceGroupService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 设备分组。分组数量有限，列表不分页。 */
@RestController
@RequestMapping("/device-group")
public class DeviceGroupController {

    private final DeviceGroupService deviceGroupService;

    public DeviceGroupController(DeviceGroupService deviceGroupService) {
        this.deviceGroupService = deviceGroupService;
    }

    @GetMapping
    @RequireModule(ModuleCode.ECS)
    public ApiResponse<List<DeviceGroupResponse>> list() {
        return ApiResponse.success(deviceGroupService.list());
    }

    @PostMapping
    @RequireModule(ModuleCode.ECS)
    public ApiResponse<DeviceGroupResponse> create(@Valid @RequestBody DeviceGroupRequest request) {
        return ApiResponse.success(deviceGroupService.create(request));
    }

    @PutMapping("/{groupId}")
    @RequireModule(ModuleCode.ECS)
    public ApiResponse<DeviceGroupResponse> update(
            @PathVariable long groupId, @Valid @RequestBody DeviceGroupUpdateRequest request) {
        return ApiResponse.success(deviceGroupService.update(groupId, request));
    }

    @DeleteMapping("/{groupId}")
    @RequireModule(ModuleCode.ECS)
    public ApiResponse<Void> delete(@PathVariable long groupId) {
        deviceGroupService.delete(groupId);
        return ApiResponse.success();
    }
}
