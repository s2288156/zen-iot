package com.zen.ecs.repository;

import com.zen.ecs.entity.DeviceCommandEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceCommandRepository extends JpaRepository<DeviceCommandEntity, Long> {

    Optional<DeviceCommandEntity> findByCommandNo(String commandNo);
}
