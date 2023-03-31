package org.zeniot.dao.repository;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.zeniot.dao.model.DeviceCredentialEntity;
import org.zeniot.dao.model.DeviceEntity;
import org.zeniot.data.enums.DeviceCredentialTypeEnum;
import org.zeniot.data.enums.DeviceStatusEnum;
import org.zeniot.data.enums.TransportTypeEnum;

import java.util.List;

/**
 * @author Wu.Chunyang
 */
@Slf4j
@SpringBootTest
class DeviceRepositoryTest {

    @Autowired
    private DeviceCredentialRepository deviceCredentialRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @BeforeEach
    void setUp() {

    }

    @AfterEach
    void tearDown() {

    }

    @Test
    void test_insert() {
        DeviceEntity deviceEntity = new DeviceEntity();
        deviceEntity.setName("test");
        deviceEntity.setStatus(DeviceStatusEnum.DISABLE);
        deviceEntity.setTransportType(TransportTypeEnum.MQTT);
        deviceRepository.save(deviceEntity);
        DeviceCredentialEntity credential = new DeviceCredentialEntity();
        credential.setDeviceId(deviceEntity.getId());
        credential.setCredentialType(DeviceCredentialTypeEnum.ACCESS_TOKEN);
        credential.setCredentialValue("111");
        deviceCredentialRepository.save(credential);
        deviceRepository.deleteById(deviceEntity.getId());
    }

    @Test
    @Order(2)
    void test_device_query_credential() {
        List<DeviceEntity> all = deviceRepository.findAll();
        DeviceEntity device = all.get(0);
        log.warn("device >>> {}", device);
        log.warn("device.credential >>> {}", device.getDeviceCredentialEntity());
    }

    @Test
    @Order(1)
    void test_deviceCredential_query_device() {
        DeviceCredentialEntity credential = deviceCredentialRepository.findAll().get(0);
        log.warn("{}", credential);
        log.warn("device {}", credential.getDeviceEntity());
    }
}
