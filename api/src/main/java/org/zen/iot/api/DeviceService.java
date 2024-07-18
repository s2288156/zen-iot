package org.zen.iot.api;

import org.zen.iot.data.base.PageQuery;
import org.zen.iot.data.base.PageResponse;
import org.zen.iot.data.domain.device.Device;

/**
 * @author Wu.Chunyang
 */
public interface DeviceService {
    PageResponse<Device> findDevices(PageQuery pageQuery);

    Device saveDevice(Device device);

    boolean deleteDevice(Long id);
}
