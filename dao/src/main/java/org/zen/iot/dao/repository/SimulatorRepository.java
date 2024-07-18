package org.zen.iot.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zen.iot.dao.model.SimulatorEntity;

public interface SimulatorRepository extends JpaRepository<SimulatorEntity, Long> {
}
