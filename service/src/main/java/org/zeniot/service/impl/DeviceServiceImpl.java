package org.zeniot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.zeniot.api.DeviceService;
import org.zeniot.dao.model.DeviceEntity;
import org.zeniot.dao.repository.DeviceRepository;
import org.zeniot.data.PageQuery;
import org.zeniot.data.PageResponse;
import org.zeniot.dto.device.Device;

import java.util.List;

/**
 * @author Wu.Chunyang
 */
@Service
public class DeviceServiceImpl implements DeviceService {
    @Autowired
    private DeviceRepository deviceRepository;

    @Override
    public PageResponse<Device> findDevices(PageQuery pageQuery) {
        Page<DeviceEntity> page = deviceRepository.findAll(pageQuery.toPageable());
        List<Device> devices = page.getContent()
                .stream()
                .map(Device::fromEntity)
                .toList();
        return PageResponse.ok(devices, page);
    }
}
