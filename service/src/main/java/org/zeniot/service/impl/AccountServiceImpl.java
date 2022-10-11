package org.zeniot.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.zeniot.api.AccountService;
import org.zeniot.common.exception.BizException;
import org.zeniot.dao.model.AccountEntity;
import org.zeniot.dao.model.RoleEntity;
import org.zeniot.dao.repository.AccountRepository;
import org.zeniot.data.enums.RoleEnums;
import org.zeniot.dto.account.Account;
import org.zeniot.dto.core.PageQuery;
import org.zeniot.dto.core.PageResponse;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author Wu.Chunyang
 */
@Slf4j
@Service
public class AccountServiceImpl implements AccountService {

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
        accountEntity.setRoles(Set.of(new RoleEntity(RoleEnums.GUEST)));
        accountRepository.save(accountEntity);
        return Account.simpleAccountFromEntity(accountEntity);
    }

    @Override
    public void deleteAccount(Long id) {
        accountRepository.deleteById(id);
    }

    @Override
    public Account findById(Long accountId) {
        Optional<AccountEntity> accountEntityOptional = accountRepository.findById(accountId);
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

    @Override
    public Account findByUsername(String username) {
        Optional<AccountEntity> accountOptional = accountRepository.findAccountByUsername(username);
        return accountOptional.map(Account::fromEntity).orElse(null);
    }

}
