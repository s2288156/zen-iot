package org.zeniot.dao.repository;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.zeniot.dao.model.DeviceCredentialEntity;
import org.zeniot.dao.model.DeviceEntity;
import org.zeniot.data.enums.DeviceCredentialTypeEnum;
import org.zeniot.data.enums.DeviceStatusEnum;
import org.zeniot.data.enums.TransportTypeEnum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

    DeviceEntity deviceEntity;

    @BeforeEach
    void setUp() {
        deviceEntity = new DeviceEntity();
        deviceEntity.setName("test");
        deviceEntity.setStatus(DeviceStatusEnum.DISABLE);
        deviceEntity.setTransportType(TransportTypeEnum.MQTT);
        deviceRepository.save(deviceEntity);
        DeviceCredentialEntity credential = new DeviceCredentialEntity();
        credential.setDeviceId(deviceEntity.getId());
        credential.setCredentialType(DeviceCredentialTypeEnum.ACCESS_TOKEN);
        credential.setCredentialValue("111");
        deviceCredentialRepository.save(credential);
    }

    @AfterEach
    void tearDown() {
        deviceRepository.deleteById(deviceEntity.getId());
    }

    @Test
    void test_device_query_credential() {
        deviceRepository.findById(deviceEntity.getId())
                .ifPresentOrElse(
                        device -> Assertions.assertAll(
                                () -> assertNotNull(device),
                                () -> assertNotNull(device.getDeviceCredentialEntity())
                        ),
                        () -> Assertions.fail("device test data is null"));
    }

    @Test
    void test_deviceCredential_query_device() {
        deviceCredentialRepository.findDeviceCredentialEntitiesByDeviceId(deviceEntity.getId())
                .ifPresentOrElse(
                        credential -> Assertions.assertAll(
                                () -> assertNotNull(credential),
                                () -> assertNotNull(credential.getDeviceEntity()),
                                () -> assertEquals(deviceEntity.getId(), credential.getDeviceEntity().getId())
                        ),
                        Assertions::fail
                );
    }
}
