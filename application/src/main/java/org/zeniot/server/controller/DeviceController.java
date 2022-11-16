package org.zeniot.server.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.zeniot.api.DeviceService;
import org.zeniot.data.PageQuery;
import org.zeniot.data.PageResponse;
import org.zeniot.data.SingleResponse;
import org.zeniot.dto.device.Device;
import org.zeniot.dto.device.DeviceCommonData;

/**
 * @author Wu.Chunyang
 */
@RequestMapping("/api")
@RestController
public class DeviceController extends AbstractController {
    @Autowired
    private DeviceService deviceService;

    @PostMapping("/device")
    public SingleResponse<Device> saveDevice(@RequestBody Device device) {
        return SingleResponse.success(deviceService.saveDevice(device));
    }

    @GetMapping("/devices")
    public PageResponse<Device> getDevices(PageQuery pageQuery) {
        return deviceService.findDevices(pageQuery);
    }

    @GetMapping("/device/common")
    public SingleResponse<DeviceCommonData> getDeviceCommonData() {
        return SingleResponse.success(DeviceCommonData.create());
    }

}
