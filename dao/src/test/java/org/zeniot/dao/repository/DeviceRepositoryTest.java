package org.zeniot.dao.repository;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.zeniot.dao.model.DeviceCredentialEntity;
import org.zeniot.dao.model.DeviceEntity;

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

    @Test
    void test_device_query_credential() {
        List<DeviceEntity> all = deviceRepository.findAll();
        DeviceEntity device = all.get(0);
        log.warn("device >>> {}", device);
        log.warn("device.credential >>> {}", device.getDeviceCredentialEntity());
    }

    @Test
    void test_deviceCredential_query_device() {
        DeviceCredentialEntity credential = deviceCredentialRepository.findAll().get(0);
        log.warn("{}", credential);
        log.warn("device {}", credential.getDeviceEntity());
    }
}
