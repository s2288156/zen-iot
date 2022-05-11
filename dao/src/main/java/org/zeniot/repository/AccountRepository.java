package org.zeniot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zeniot.model.Account;

import java.util.Optional;

/**
 * @author Wu.Chunyang
 */
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findAccountByUsername(String username);
}