package org.zeniot.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zeniot.dao.model.AccountEntity;
import org.zeniot.dao.model.RoleEntity;

import java.util.Optional;

/**
 * @author Wu.Chunyang
 */
public interface RoleRepository extends JpaRepository<RoleEntity, Long> {


}
