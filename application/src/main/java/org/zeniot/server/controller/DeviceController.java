package org.zeniot.server.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zeniot.api.DeviceService;
import org.zeniot.data.SingleResponse;
import org.zeniot.dto.device.Device;

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

}
