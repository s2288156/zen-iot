package org.zeniot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.zeniot.api.DeviceService;
import org.zeniot.dao.repository.DeviceRepository;

/**
 * @author Wu.Chunyang
 */
@Service
public class DeviceServiceImpl implements DeviceService {
    @Autowired
    private DeviceRepository deviceRepository;
}
