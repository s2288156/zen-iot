package org.zeniot.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.zeniot.model.Account;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Wu.Chunyang
 */
@SpringBootTest
class AccountRepositoryTest {
    @Autowired
    private AccountRepository accountRepository;

    @Test
    void add_one_account() {
        Account account = new Account("laowang", "123456");
        Account savedAccount = accountRepository.save(account);
        assertEquals(account.getId(), savedAccount.getId());

    }
}