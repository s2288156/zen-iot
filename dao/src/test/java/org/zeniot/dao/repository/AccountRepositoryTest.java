package org.zeniot.dao.repository;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.zeniot.dao.model.AccountEntity;
import org.zeniot.dao.model.RoleEntity;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

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
        admin.setRoles(Set.of(allRole.get(0)));
        accountRepository.save(admin);
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void findAccountByUsername() {
        Optional<AccountEntity> account = accountRepository.findAccountByUsername("admin");
        log.warn("####### {}", account.get());
    }

    @Test
    void existsAccountEntityByUsername() {
        List<RoleEntity> all = roleRepository.findAll();
        log.warn("{}", all);
    }
}
