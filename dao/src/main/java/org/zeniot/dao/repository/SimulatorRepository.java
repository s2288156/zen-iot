package org.zeniot.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zeniot.dao.model.SimulatorEntity;

public interface SimulatorRepository extends JpaRepository<SimulatorEntity, Long> {
}
