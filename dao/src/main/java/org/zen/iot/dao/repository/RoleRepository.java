package org.zen.iot.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zen.iot.dao.model.RoleEntity;
import org.zen.iot.data.enums.RoleEnum;

import java.util.Optional;

/**
 * @author Wu.Chunyang
 */
public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

    Optional<RoleEntity> findByRoleName(RoleEnum roleName);

}
