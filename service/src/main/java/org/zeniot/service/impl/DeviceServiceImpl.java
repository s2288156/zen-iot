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
import org.zeniot.mapper.DeviceMapper;

import java.util.List;

/**
 * @author Wu.Chunyang
 */
@Service
public class DeviceServiceImpl implements DeviceService {
    @Autowired
    private DeviceRepository deviceRepository;
    @Autowired
    private DeviceMapper mapper;

    @Override
    public PageResponse<Device> findDevices(PageQuery pageQuery) {
        Page<DeviceEntity> page = deviceRepository.findAll(pageQuery.toPageable());
        List<Device> devices = page.getContent()
                .stream()
                .map(entity -> mapper.entityToDevice(entity))
                .toList();
        return PageResponse.ok(devices, page);
    }

    @Override
    public Device saveDevice(Device device) {
        DeviceEntity deviceEntity = mapper.deviceToEntity(device);
        deviceRepository.save(deviceEntity);
        return null;
    }


}
