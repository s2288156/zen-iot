package org.zeniot.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zeniot.dao.model.AccountEntity;

import java.util.Optional;

/**
 * @author Wu.Chunyang
 */
public interface AccountRepository extends JpaRepository<AccountEntity, Long> {

    Optional<AccountEntity> findAccountByUsername(String username);
}