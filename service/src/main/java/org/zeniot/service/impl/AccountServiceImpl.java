package org.zeniot.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.zeniot.api.AccountService;
import org.zeniot.common.exception.BizException;
import org.zeniot.dao.model.AccountEntity;
import org.zeniot.dao.model.RoleEntity;
import org.zeniot.dao.repository.AccountRepository;
import org.zeniot.dao.repository.RoleRepository;
import org.zeniot.data.base.PageQuery;
import org.zeniot.data.base.PageResponse;
import org.zeniot.data.enums.RoleEnum;
import org.zeniot.data.domain.account.Account;
import org.zeniot.service.mapper.AccountMapper;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author Wu.Chunyang
 */
@Slf4j
@Service
public class AccountServiceImpl implements AccountService {

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private AccountMapper accountMapper;

    @Override
    public Account registerAccount(Account account) {
        if (accountRepository.existsAccountEntityByUsername(account.getUsername())) {
            throw new BizException("username existed!");
        }
        Optional<RoleEntity> defaultRole = roleRepository.findByRoleName(RoleEnum.GUEST);
        AccountEntity accountEntity = AccountEntity.toEntity(passwordEncoder, account);
        defaultRole.ifPresent(roleEntity -> accountEntity.setRoleEntities(Set.of(roleEntity)));
        accountRepository.save(accountEntity);
        return accountMapper.entityToSimpleAccount(accountEntity);
    }

    @Override
    public void deleteAccount(Long id) {
        accountRepository.deleteById(id);
    }

    @Override
    public Account findById(Long accountId) {
        Optional<AccountEntity> accountEntityOptional = accountRepository.findById(accountId);
        return accountEntityOptional.map(accountMapper::entityToSimpleAccount).orElse(null);
    }

    @Override
    public PageResponse<Account> pageAll(PageQuery pageQuery) {
        Page<AccountEntity> all = accountRepository.findAll(pageQuery.toPageable());
        List<Account> accounts = all.getContent()
                .stream()
                .map(accountMapper::entityToSimpleAccount)
                .toList();
        return PageResponse.ok(accounts, all);
    }

    @Override
    public Account findByUsername(String username) {
        Optional<AccountEntity> accountOptional = accountRepository.findAccountByUsername(username);
        return accountOptional.map(AccountEntity::toAccount).orElse(null);
    }

}
