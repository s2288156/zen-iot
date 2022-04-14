package org.zeniot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zeniot.model.Account;

/**
 * @author Wu.Chunyang
 */
public interface AccountRepository extends JpaRepository<Account, Long> {
}
