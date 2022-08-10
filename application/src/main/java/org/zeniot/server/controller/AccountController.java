package org.zeniot.server.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.zeniot.dao.id.AccountId;
import org.zeniot.server.controller.response.PageResponse;
import org.zeniot.server.controller.response.RestResponse;
import org.zeniot.server.dto.PageQuery;
import org.zeniot.server.dto.account.Account;
import org.zeniot.server.service.AccountService;

/**
 * @author Wu.Chunyang
 */
@RequestMapping("/api")
@RestController
public class AccountController {

    @Autowired
    private AccountService accountService;

    public RestResponse<Account> login(Account account) {
        return RestResponse.ok();
    }

    @PostMapping("/account/register")
    public RestResponse<Account> registerAccount(@Validated @RequestBody Account account) {
        return RestResponse.success(accountService.registerAccount(account));
    }

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
