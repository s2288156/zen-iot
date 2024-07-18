package org.zen.iot.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zen.iot.dao.model.DeviceEntity;

/**
 * @author Wu.Chunyang
 */
public interface DeviceRepository extends JpaRepository<DeviceEntity, Long> {
}
