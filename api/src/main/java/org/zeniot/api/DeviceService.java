package org.zeniot.api;

import org.zeniot.data.base.PageQuery;
import org.zeniot.data.base.PageResponse;
import org.zeniot.data.domain.device.Device;

/**
 * @author Wu.Chunyang
 */
public interface DeviceService {
    PageResponse<Device> findDevices(PageQuery pageQuery);

    Device saveDevice(Device device);

    boolean deleteDevice(Long id);
}
