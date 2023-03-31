package org.zeniot.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zeniot.dao.model.DeviceCredentialEntity;

public interface DeviceCredentialRepository extends JpaRepository<DeviceCredentialEntity, Long> {
}
