package org.zeniot.server.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.zeniot.dao.id.AccountId;
import org.zeniot.service.api.AccountService;
import org.zeniot.service.dto.account.Account;
import org.zeniot.service.dto.core.PageQuery;
import org.zeniot.service.dto.core.PageResponse;
import org.zeniot.service.dto.core.RestResponse;

/**
 * @author Wu.Chunyang
 */
@RequestMapping("/api")
@RestController
public class AccountController extends AbstractController {

    @Autowired
    private AccountService accountService;

    public RestResponse<Account> login(Account account) {
        return RestResponse.ok();
    }

    @PostMapping("/account/register")
    public RestResponse<Account> registerAccount(@Validated @RequestBody Account account) {
        return RestResponse.success(accountService.registerAccount(account));
    }

    @PreAuthorize("hasAnyAuthority(sd.role.ADMIN)")
    @DeleteMapping("/account/{id}")
    public RestResponse<Account> deleteAccount(@PathVariable Long id) {
        accountService.deleteAccount(id);
        return RestResponse.ok();
    }

    @GetMapping("/account/{id}")
    public RestResponse<Account> findAccountById(@PathVariable Long id) {
        Account account = accountService.findById(AccountId.of(id));
        return RestResponse.success(account);
    }

    @GetMapping("/accounts")
    public PageResponse<Account> findAccounts(@Validated PageQuery query) {
        return accountService.pageAll(query);
    }

}
