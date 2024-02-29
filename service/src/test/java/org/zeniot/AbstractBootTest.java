package org.zeniot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.zeniot.api.DeviceService;
import org.zeniot.dao.repository.DeviceRepository;

/**
 * @author Wu.Chunyang
 */
@ActiveProfiles(profiles = "test")
@SpringBootTest
public class AbstractBootTest {
    @Autowired
    public DeviceService deviceService;
    @Autowired
    public DeviceRepository deviceRepository;
}
