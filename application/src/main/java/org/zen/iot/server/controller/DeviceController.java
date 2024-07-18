package org.zen.iot.server.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.zen.iot.api.DeviceService;
import org.zen.iot.data.base.PageQuery;
import org.zen.iot.data.base.PageResponse;
import org.zen.iot.data.base.SingleResponse;
import org.zen.iot.data.command.DeviceStatusSwitchCmd;
import org.zen.iot.data.domain.device.Device;
import org.zen.iot.data.domain.device.DeviceCommonData;

/**
 * @author Wu.Chunyang
 */
@RequestMapping("/api")
@RestController
public class DeviceController extends AbstractController {
    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping("/device")
    public SingleResponse<Device> saveDevice(@Validated @RequestBody Device device) {
        return SingleResponse.success(deviceService.saveDevice(device));
    }

    @GetMapping("/devices")
    public PageResponse<Device> getDevices(@Validated PageQuery pageQuery) {
        return deviceService.findDevices(pageQuery);
    }

    @GetMapping("/device/common")
    public SingleResponse<DeviceCommonData> getDeviceCommonData() {
        return SingleResponse.success(DeviceCommonData.create());
    }

    @DeleteMapping("/device/{id}")
    public SingleResponse<Device> deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return SingleResponse.success();
    }

    @PostMapping("/device/switch")
    public SingleResponse<?> switchStatus(@Validated @RequestBody DeviceStatusSwitchCmd cmd) {

        return SingleResponse.success();
    }
}
