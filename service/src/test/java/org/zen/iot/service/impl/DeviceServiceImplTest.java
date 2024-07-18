package org.zen.iot.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zen.iot.AbstractBootTest;
import org.zen.iot.dao.model.DeviceEntity;
import org.zen.iot.data.base.PageQuery;
import org.zen.iot.data.base.PageResponse;
import org.zen.iot.data.domain.device.Device;
import org.zen.iot.data.enums.DeviceStatusEnum;
import org.zen.iot.data.enums.TransportTypeEnum;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Wu.Chunyang
 */
@Slf4j
class DeviceServiceImplTest extends AbstractBootTest {

    private Device d1, d2;

    @BeforeEach
    void setUp() {
        deviceRepository.deleteAll();
        d1 = Device.builder()
                .name("d1").status(DeviceStatusEnum.ENABLE).transportType(TransportTypeEnum.MQTT)
                .build();
        d2 = Device.builder()
                .name("d2").status(DeviceStatusEnum.DISABLE).transportType(TransportTypeEnum.MQTT)
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
        Device device1 = deviceService.saveDevice(d1);
        assertEquals(1, deviceRepository.findAll().size());
        assertTrue(deviceService.deleteDevice(device1.getId()));
        assertEquals(0, deviceRepository.findAll().size());
    }
}
