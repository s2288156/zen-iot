package org.zeniot.dao.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.zeniot.dao.domain.Account;
import org.zeniot.dao.domain.Customer;

import javax.persistence.Id;

/**
 * @author Wu.Chunyang
 */
public interface AccountRepository extends JpaRepository<Account, Id> {
    @Query("SELECT a.customer FROM Account a WHERE a.name = :name")
    Customer findCustomerBy(@Param("name") String name);
}
