package org.zen.iot.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zen.iot.dao.model.AccountEntity;

import java.util.Optional;

/**
 * @author Wu.Chunyang
 */
public interface AccountRepository extends JpaRepository<AccountEntity, Long> {

    Optional<AccountEntity> findAccountByUsername(String username);

    boolean existsAccountEntityByUsername(String username);

}
