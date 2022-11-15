package org.zeniot.api;

import org.zeniot.data.PageQuery;
import org.zeniot.data.PageResponse;
import org.zeniot.dto.device.Device;

/**
 * @author Wu.Chunyang
 */
public interface DeviceService {
    PageResponse<Device> findDevices(PageQuery pageQuery);

    Long saveDevice(Device device);

    boolean deleteDevice(Long id);
}
