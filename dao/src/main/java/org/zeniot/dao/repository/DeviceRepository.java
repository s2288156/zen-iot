package org.zeniot.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zeniot.dao.model.DeviceEntity;

/**
 * @author Wu.Chunyang
 */
public interface DeviceRepository extends JpaRepository<DeviceEntity, Long> {
}
