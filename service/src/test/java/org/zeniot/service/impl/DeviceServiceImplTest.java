package org.zeniot.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.zeniot.AbstractBootTest;
import org.zeniot.api.DeviceService;
import org.zeniot.data.PageQuery;
import org.zeniot.data.PageResponse;
import org.zeniot.data.enums.DeviceStatusEnum;
import org.zeniot.data.enums.DeviceTransportTypeEnum;
import org.zeniot.dto.device.Device;

/**
 * @author Wu.Chunyang
 */
@Slf4j
class DeviceServiceImplTest extends AbstractBootTest {
    @Autowired
    private DeviceService deviceService;

    @BeforeEach
    void setUp() {
        Device d1 = Device.builder()
                .name("d1")
                .status(DeviceStatusEnum.ENABLE)
                .transportType(DeviceTransportTypeEnum.MQTT)
                .build();
        deviceService.saveDevice(d1);
    }

    @Test
    void findDevices() {
        PageQuery pageQuery = new PageQuery();
        pageQuery.setPage(0);
        pageQuery.setSize(5);
        PageResponse<Device> devices = deviceService.findDevices(pageQuery);
        log.warn("@@@@ {}", devices.getData());
    }
}
