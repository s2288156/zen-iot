package org.zeniot.server.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.zeniot.dao.id.AccountId;
import org.zeniot.dao.model.AccountEntity;
import org.zeniot.dao.model.RoleEntity;
import org.zeniot.dao.repository.AccountRepository;
import org.zeniot.server.controller.response.PageResponse;
import org.zeniot.server.dto.PageQuery;
import org.zeniot.server.dto.account.Account;
import org.zeniot.server.exception.BizException;
import org.zeniot.server.service.AccountService;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author Wu.Chunyang
 */
@Slf4j
@Service
public class AccountServiceImpl implements AccountService {

    public static final String DEFAULT_ROLE_ADMIN = "ADMIN";
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountServiceImpl(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Account registerAccount(Account account) {
        if (accountRepository.existsAccountEntityByUsername(account.getUsername())) {
            throw new BizException("username existed!");
        }
        AccountEntity accountEntity = account.toEntity(passwordEncoder);
        accountEntity.setRoles(Set.of(new RoleEntity(DEFAULT_ROLE_ADMIN)));
        accountRepository.save(accountEntity);
        return Account.simpleAccountFromEntity(accountEntity);
    }

    @Override
    public void deleteAccount(Long id) {
        accountRepository.deleteById(id);
    }

    @Override
    public Account findById(AccountId accountId) {
        Optional<AccountEntity> accountEntityOptional = accountRepository.findById(accountId.getId());
        return accountEntityOptional.map(Account::simpleAccountFromEntity).orElse(null);
    }

    @Override
    public PageResponse<Account> pageAll(PageQuery pageQuery) {
        PageRequest pageable = PageRequest.of(pageQuery.getPage(), pageQuery.getSize());
        Page<AccountEntity> all = accountRepository.findAll(pageable);
        int totalPages = all.getTotalPages();
        long totalElements = all.getTotalElements();
        List<Account> accounts = all.getContent()
                .stream()
                .map(Account::simpleAccountFromEntity)
                .toList();
        return PageResponse.of(accounts, all.getSize(), totalPages, totalElements, all.hasNext());
    }
}
