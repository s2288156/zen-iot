package org.zeniot.server.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.zeniot.dao.repository.AccountRepository;
import org.zeniot.server.controller.response.Account;
import org.zeniot.server.service.AccountService;

/**
 * @author Wu.Chunyang
 */
@Service
public class AccountServiceImpl implements AccountService {

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void registerAccount(Account account) {
        accountRepository.save(account.toEntity(passwordEncoder));
    }

    @Override
    public void deleteAccount(Long id) {
        accountRepository.deleteById(id);
    }
}
