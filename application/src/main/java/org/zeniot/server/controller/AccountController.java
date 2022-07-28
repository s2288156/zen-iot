package org.zeniot.server.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.zeniot.server.controller.response.Account;
import org.zeniot.server.controller.response.PageResponse;
import org.zeniot.server.controller.response.RestResponse;
import org.zeniot.server.service.AccountService;

/**
 * @author Wu.Chunyang
 */
@RequestMapping("/api")
@RestController
public class AccountController {

    @Autowired
    private AccountService accountService;

    @PostMapping("/account/register")
    public RestResponse<Account> registerAccount(@Validated @RequestBody Account account) {
        accountService.registerAccount(account);
        return RestResponse.ok();
    }

    @DeleteMapping("/account/{id}")
    public RestResponse<Account> deleteAccount(@PathVariable Long id) {
        accountService.deleteAccount(id);
        return RestResponse.ok();
    }

    @GetMapping("/accounts")
    public PageResponse<Account> findAccounts() {
        return accountService.pageAll();
    }

}
