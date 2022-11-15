package org.zeniot.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.zeniot.AbstractBootTest;
import org.zeniot.api.DeviceService;
import org.zeniot.dao.model.DeviceEntity;
import org.zeniot.dao.repository.DeviceRepository;
import org.zeniot.data.PageQuery;
import org.zeniot.data.PageResponse;
import org.zeniot.data.enums.DeviceStatusEnum;
import org.zeniot.data.enums.DeviceTransportTypeEnum;
import org.zeniot.dto.device.Device;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Wu.Chunyang
 */
@Slf4j
class DeviceServiceImplTest extends AbstractBootTest {
    @Autowired
    private DeviceService deviceService;
    @Autowired
    private DeviceRepository deviceRepository;

    private Device d1, d2;

    @BeforeEach
    void setUp() {
        deviceRepository.deleteAll();
        d1 = Device.builder()
                .name("d1").status(DeviceStatusEnum.ENABLE).transportType(DeviceTransportTypeEnum.MQTT)
                .build();
        d2 = Device.builder()
                .name("d2").status(DeviceStatusEnum.DISABLE).transportType(DeviceTransportTypeEnum.MQTT)
                .build();
    }

    @Test
    void test_findDevices() {
        deviceService.saveDevice(d1);
        deviceService.saveDevice(d2);
        PageQuery pageQuery = new PageQuery(0, 5);
        PageResponse<Device> devices = deviceService.findDevices(pageQuery);
        assertEquals(2, devices.getTotalElements());
    }

    @Test
    void test_saveDevice() {
        assertNotNull(deviceService.saveDevice(d1));
        List<DeviceEntity> all = deviceRepository.findAll();
        assertNotNull(all);
        assertEquals(1, all.size());

        assertNotNull(deviceService.saveDevice(d2));
        all = deviceRepository.findAll();
        assertEquals(2, all.size());
    }

    @Test
    void test_deleteDevice() {
        Long id1 = deviceService.saveDevice(d1);
        assertEquals(1, deviceRepository.findAll().size());
        assertTrue(deviceService.deleteDevice(id1));
        assertEquals(0, deviceRepository.findAll().size());
    }
}
