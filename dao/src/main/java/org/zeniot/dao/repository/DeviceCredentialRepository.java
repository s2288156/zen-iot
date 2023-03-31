package org.zeniot.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zeniot.dao.model.DeviceCredentialEntity;

import java.util.Optional;

public interface DeviceCredentialRepository extends JpaRepository<DeviceCredentialEntity, Long> {
    Optional<DeviceCredentialEntity> findDeviceCredentialEntitiesByDeviceId(Long deviceId);
}
