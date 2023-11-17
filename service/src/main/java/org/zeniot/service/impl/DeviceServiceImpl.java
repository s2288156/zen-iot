package org.zeniot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.zeniot.api.DeviceService;
import org.zeniot.dao.model.DeviceEntity;
import org.zeniot.dao.repository.DeviceRepository;
import org.zeniot.data.base.PageQuery;
import org.zeniot.data.base.PageResponse;
import org.zeniot.data.domain.device.Device;
import org.zeniot.data.enums.DeviceStatusEnum;
import org.zeniot.service.mapper.DeviceMapper;

import java.util.List;

/**
 * @author Wu.Chunyang
 */
@Service
public class DeviceServiceImpl implements DeviceService {
    @Autowired
    private DeviceRepository deviceRepository;
    @Autowired
    private DeviceMapper deviceMapper;

    @Override
    public PageResponse<Device> findDevices(PageQuery pageQuery) {
        Page<DeviceEntity> page = deviceRepository.findAll(pageQuery.toPageable());
        List<Device> devices = page.getContent().stream()
                .map(entity -> deviceMapper.entityToDevice(entity))
                .toList();
        return PageResponse.success(devices, page);
    }

    @Override
    public Device saveDevice(Device device) {
        if (device.getId() == null) {
            device.setStatus(DeviceStatusEnum.DISABLE);
        }
        DeviceEntity deviceEntity = deviceMapper.deviceToEntity(device);
        deviceRepository.save(deviceEntity);
        return deviceMapper.entityToDevice(deviceEntity);
    }

    @Override
    public boolean deleteDevice(Long id) {
        deviceRepository.deleteById(id);
        return true;
    }

}
