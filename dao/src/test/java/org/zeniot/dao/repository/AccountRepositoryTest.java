package org.zeniot.dao.repository;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.zeniot.dao.model.AccountEntity;
import org.zeniot.dao.model.RoleEntity;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Wu.Chunyang
 */
@Slf4j
@DataJpaTest
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void setUp() {
        List<RoleEntity> allRole = roleRepository.findAll();
        AccountEntity admin = new AccountEntity();
        admin.setUsername("admin");
        admin.setPassword("123123");
        admin.setRoleEntities(Set.of(allRole.get(0)));
        accountRepository.save(admin);
    }

    @AfterEach
    void tearDown() {
    }

    @DisplayName("find account by username")
    @Test
    void findAccountByUsername() {
        Optional<AccountEntity> account = accountRepository.findAccountByUsername("admin");
        assertTrue(account.isPresent());
        assertEquals(1, account.get().getRoleEntities().size());
    }

    @Test
    void existsAccountEntityByUsername() {
        List<RoleEntity> allRule = roleRepository.findAll();
        List<AccountEntity> allAccount = accountRepository.findAll();
        log.warn("{}", allRule);
        log.warn("{}", allAccount);
    }

}
