package org.zeniot.dao.repository;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.zeniot.dao.model.AccountEntity;

/**
 * @author Wu.Chunyang
 */
@SpringBootTest
public class TestDataCreator {
    @Autowired
    private AccountRepository accountRepository;

    @Disabled
    @Test
    void insertTestData() {
        for (int i = 0; i < 20; i++) {
            AccountEntity accountEntity = new AccountEntity();
            accountEntity.setUsername("test" + i);
            accountEntity.setPassword("pwd");
            accountRepository.save(accountEntity);
        }
    }
}
